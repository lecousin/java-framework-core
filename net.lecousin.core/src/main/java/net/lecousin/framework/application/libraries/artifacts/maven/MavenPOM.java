package net.lecousin.framework.application.libraries.artifacts.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionRange;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromURL;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.SystemEnvironment;
import net.lecousin.framework.util.SystemEnvironment.OSFamily;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;
import net.lecousin.framework.xml.XMLStreamReader;

/**
 * Implementation of LibraryDescriptor from a Maven POM file.
 */
public class MavenPOM implements LibraryDescriptor {
	
	/** Load a POM file. */
	@SuppressWarnings("resource")
	public static AsyncWork<MavenPOM, Exception> load(URL pomFile, byte priority, MavenPOMLoader pomLoader, boolean fromRepository) {
		AsyncWork<MavenPOM, Exception> result = new AsyncWork<>();
		IOProvider p = IOProviderFromURL.getInstance().get(pomFile);
		if (p == null)
			return new AsyncWork<>(null, new FileNotFoundException(pomFile.toString()));
		if (!(p instanceof IOProvider.Readable))
			return new AsyncWork<>(null, new IOException("File not readable: " + pomFile.toString()));
		IO.Readable fileIO;
		try { fileIO = ((IOProvider.Readable)p).provideIOReadable(priority); }
		catch (IOException e) {
			return new AsyncWork<>(null, e);
		}
		Task<Void, NoException> task = new Task.Cpu<Void, NoException>("Loading POM", priority) {
			@SuppressWarnings("unchecked")
			@Override
			public Void run() {
				AsyncWork<? extends IO.Readable.Buffered, IOException> readFile;
				int bufSize;
				if (fileIO instanceof IO.KnownSize) {
					try { bufSize = (int)((IO.KnownSize)fileIO).getSizeSync(); }
					catch (IOException e) {
						result.error(e);
						return null;
					}
					byte[] buf = new byte[bufSize];
					readFile = new AsyncWork<ByteArrayIO, IOException>();
					fileIO.readFullyAsync(ByteBuffer.wrap(buf)).listenInline(() -> {
						((AsyncWork<ByteArrayIO, IOException>)readFile).unblockSuccess(
							new ByteArrayIO(buf, pomFile.toString()));
					}, readFile);
				} else {
					bufSize = 4096;
					readFile = IOUtil.readFullyAsync(fileIO, 4096);
				}
				MavenPOM pom = new MavenPOM(pomLoader, pomFile);
				readFile.listenAsync(new Task.Cpu.FromRunnable("Loading POM", priority, () -> {
					fileIO.closeAsync();
					if (readFile.hasError()) {
						result.error(readFile.getError());
						return;
					}
					IO.Readable.Buffered bio = readFile.getResult();
					AsyncWork<XMLStreamReader, Exception> startXMLReader = XMLStreamReader.start(bio, bufSize);
					Reader read = pom.new Reader(startXMLReader, priority, fromRepository, pomFile, pomLoader);
					read.startOn(startXMLReader, true);
					read.getOutput().listenInline(() -> {
						if (pom.parentLoading == null) {
							pom.new Finalize(result, priority).start();
							return;
						}
						pom.parentLoading.listenInline(() -> { pom.new Finalize(result, priority).start(); }, result);
					}, result);
				}), true);
				return null;
			}
		};
		task.startOn(fileIO.canStartReading(), true);
		return result;
	}
	
	private MavenPOM(MavenPOMLoader loader, URL pomFile) {
		this.loader = loader;
		this.pomFile = pomFile;
	}
	
	private MavenPOMLoader loader;
	private URL pomFile;
	
	private String artifactId;
	private String groupId;
	private String version;
	private String packaging;
	
	private String parentArtifactId;
	private String parentGroupId;
	private String parentVersion;
	private String parentRelativePath = "../pom.xml";
	private AsyncWork<MavenPOM, Exception> parentLoading;
	
	private static class Build {
		private String outputDirectory;
	}
	
	private Build build = new Build();
	
	private Map<String, String> properties = new HashMap<>();
	
	private List<Dependency> dependencies = new LinkedList<>();
	private List<Dependency> dependencyManagement = new LinkedList<>();
	
	/** Dependency specified in the POM file. */
	public class Dependency implements LibraryDescriptor.Dependency {
		private String groupId;
		private String artifactId;
		private String version;
		private String type;
		private String classifier;
		private String scope;
		private String systemPath;
		private boolean optional = false;
		private List<Pair<String, String>> exclusions = new LinkedList<>();
		
		@Override
		public String getGroupId() { return groupId; }
		
		@Override
		public String getArtifactId() { return artifactId; }
		
		@Override
		public VersionSpecification getVersionSpecification() {
			return parseVersionSpecification(version);
		}
		
		@Override
		public String getClassifier() { return classifier; }
		
		@Override
		public boolean isOptional() { return optional; }
		
		@Override
		public URL getKnownLocation() {
			if (systemPath == null) return null;
			try {
				URI uri = new URI(systemPath);
				if (uri.isAbsolute())
					return uri.toURL();
				URI pomURI = pomFile.toURI();
				URI parent = pomURI.resolve(".");
				return parent.resolve(systemPath).toURL(); 
			} catch (Exception e) {
				return null;
			}
		}
		
		@Override
		public List<Pair<String, String>> getExcludedDependencies() {
			return exclusions;
		}
	}
	
	private static class Repository {
		private String url;
		private boolean releasesEnabled = true;
		private boolean snapshotsEnabled = true;
	}
	
	private List<Repository> repositories = new LinkedList<>();
	
	@Override
	public List<LibrariesRepository> getDependenciesAdditionalRepositories() {
		return getRepositories(repositories);
	}
	
	private List<LibrariesRepository> getRepositories(List<Repository> repos) {
		ArrayList<LibrariesRepository> list = new ArrayList<>(repos.size());
		for (Repository r : repos) {
			if (r.url == null || r.url.trim().isEmpty())
				continue;
			MavenRepository repo = loader.getRepository(r.url, r.releasesEnabled, r.snapshotsEnabled);
			if (repo != null)
				list.add(repo);
		}
		return list;
	}
	
	private static class Profile {
		private boolean activeByDefault = false;
		private String jdk = null;
		private ActivationOS activationOS;
		private String activationPropertyName;
		private String activationPropertyValue;
		private String activationMissingFile;
		private String activationFileExists;
		
		private Build build = new Build();
		private Map<String, String> properties = new HashMap<>();
		private List<Dependency> dependencyManagement = new LinkedList<>();
		private List<Dependency> dependencies = new LinkedList<>();
		private List<Repository> repositories = new LinkedList<>();
	}
	
	private static class ActivationOS {
		private String name;
		private String family;
		private String arch;
		private String version;
	}
	
	private List<Profile> profiles = new LinkedList<>();

	@Override
	public MavenPOMLoader getLoader() {
		return loader;
	}
	
	@Override
	public String getGroupId() { return groupId; }
	
	@Override
	public String getArtifactId() { return artifactId; }
	
	@Override
	public String getVersionString() { return version; }
	
	@Override
	public Version getVersion() {
		return new Version(version);
	}
	
	@Override
	public URL getDirectory() {
		try {
			return pomFile.toURI().resolve(".").toURL();
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public boolean hasClasses() {
		return "jar".equals(packaging) || "bundle".equals(packaging);
	}
	
	private File classesFile = null;
	
	@SuppressWarnings("resource")
	@Override
	public AsyncWork<File, NoException> getClasses() {
		if (!hasClasses()) return new AsyncWork<>(null, null);
		if (classesFile != null) return new AsyncWork<>(classesFile, null);
		if ("file".equals(pomFile.getProtocol())) {
			File pf;
			try { pf = new File(pomFile.toURI()); }
			catch (URISyntaxException e) {
				return new AsyncWork<>(null, null);
			}
			pf = pf.getParentFile();
			if (build.outputDirectory != null) {
				File dir = new File(build.outputDirectory);
				if (dir.isAbsolute())
					classesFile = dir;
				else
					classesFile = new File(pf, build.outputDirectory);
			} else {
				File f = new File(pf, artifactId + '-' + version + ".jar");
				if (f.exists()) classesFile = f;
				else {
					f = new File(pf, "target/classes");
					if (f.exists()) classesFile = f;
				}
			}
			return new AsyncWork<>(classesFile, null);
		}
		try {
			URI uri = pomFile.toURI().resolve(".").resolve(artifactId + '-' + version + ".jar");
			IOProvider p = IOProviderFromURL.getInstance().get(uri.toURL());
			if (p == null || !(p instanceof IOProvider.Readable))
				return new AsyncWork<>(null, null);
			IO.Readable io = ((IOProvider.Readable)p).provideIOReadable(Task.PRIORITY_IMPORTANT);
			if (io == null)
				return new AsyncWork<>(null, null);
			AsyncWork<File, IOException> download = IOUtil.toTempFile(io);
			AsyncWork<File, NoException> result = new AsyncWork<>();
			download.listenInline(() -> {
				if (download.isSuccessful())
					result.unblockSuccess(classesFile = download.getResult());
				else
					result.unblockSuccess(null);
				
			});
			return result;
		} catch (Exception e) {
			return new AsyncWork<>(null, null);
		}
	}
	
	@Override
	public List<LibraryDescriptor.Dependency> getDependencies() {
		ArrayList<LibraryDescriptor.Dependency> list = new ArrayList<>(dependencies.size());
		for (Dependency dep : dependencies) {
			if (dep.scope == null || "compile".equals(dep.scope))
				list.add(dep);
		}
		return list;
	}

	private class Reader extends Task.Cpu<Void, Exception> {
		private Reader(AsyncWork<XMLStreamReader, Exception> startXMLReader, byte priority,
			boolean fromRepository, URL pomFile, MavenPOMLoader pomLoader) {
			super("Read POM " + pomFile.toString(), priority);
			this.startXMLReader = startXMLReader;
			this.fromRepository = fromRepository;
			this.pomFile = pomFile;
			this.pomLoader = pomLoader;
		}
		
		private AsyncWork<XMLStreamReader, Exception> startXMLReader;
		private boolean fromRepository;
		private URL pomFile;
		private MavenPOMLoader pomLoader;
		
		@Override
		public Void run() throws Exception {
			if (startXMLReader.hasError()) throw startXMLReader.getError();
			if (startXMLReader.isCancelled()) throw startXMLReader.getCancelEvent();
			XMLStreamReader xml = startXMLReader.getResult();
			while (!Type.START_ELEMENT.equals(xml.event.type)) xml.next();
			if (!xml.event.text.equals("project"))
				throw new Exception("Invalid POM: root element must be a project");
			while (xml.nextStartElement()) {
				if (xml.event.text.equals("artifactId"))
					artifactId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("groupId"))
					groupId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("version"))
					version = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("packaging"))
					packaging = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("parent"))
					readParent(xml);
				else if (xml.event.text.equals("build"))
					readBuild(xml, build);
				else if (xml.event.text.equals("properties"))
					readProperties(xml, properties);
				else if (xml.event.text.equals("dependencyManagement"))
					readDependencyManagement(xml, dependencyManagement);
				else if (xml.event.text.equals("dependencies"))
					readDependencies(xml, dependencies);
				else if (xml.event.text.equals("profiles"))
					readProfiles(xml);
				else if (xml.event.text.equals("repositories"))
					readRepositories(xml, repositories);
				else
					xml.closeElement();
			}
			if (parentGroupId != null) {
				if (!fromRepository) {
					File parentFile = new File(new File(pomFile.toURI()).getParentFile(), parentRelativePath);
					if (parentFile.exists()) {
						if (parentFile.isDirectory()) {
							parentFile = new File(parentFile, "pom.xml");
							if (parentFile.exists())
								parentLoading = pomLoader.loadPOM(parentFile.toURI().toURL(), getPriority());
						} else
							parentLoading = pomLoader.loadPOM(parentFile.toURI().toURL(), getPriority());
					}
				}
				if (parentLoading == null) {
					// TODO try to resolve properties ?
					parentLoading = pomLoader.loadLibrary(
						parentGroupId, parentArtifactId, new VersionSpecification.SingleVersion(new Version(parentVersion)),
						getPriority(), getRepositories(repositories));
				}
			}
			return null;
		}
		
		private void readParent(XMLStreamReader xml) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing parent tag");
					break;
				}
				if (xml.event.text.equals("artifactId"))
					parentArtifactId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("groupId"))
					parentGroupId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("version"))
					parentVersion = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("relativePath"))
					parentRelativePath = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			} while (true);
		}

		private void readBuild(XMLStreamReader xml, Build build) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing build tag");
					break;
				}
				if (xml.event.text.equals("outputDirectory"))
					build.outputDirectory = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			} while (true);
		}

		private void readProperties(XMLStreamReader xml, Map<String, String> properties) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing properties tag");
					break;
				}
				properties.put(xml.event.text.asString(), xml.readInnerText().asString());
			} while (true);
		}

		private void readDependencyManagement(XMLStreamReader xml, List<Dependency> dependencyManagement) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing dependencyManagement tag");
					break;
				}
				if (!xml.event.text.equals("dependencies")) {
					xml.closeElement();
					continue;
				}
				readDependencies(xml, dependencyManagement);
			} while (true);
		}

		private void readDependencies(XMLStreamReader xml, List<Dependency> dependencies) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing dependencies tag");
					break;
				}
				if (!xml.event.text.equals("dependency")) {
					xml.closeElement();
					continue;
				}
				Dependency dep = new Dependency();
				ElementContext depCtx = xml.event.context.getFirst();
				while (xml.nextInnerElement(depCtx)) {
					if (xml.event.text.equals("groupId"))
						dep.groupId = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("artifactId"))
						dep.artifactId = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("version"))
						dep.version = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("type"))
						dep.type = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("classifier"))
						dep.classifier = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("scope"))
						dep.scope = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("systemPath"))
						dep.systemPath = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals("optional"))
						dep.optional = xml.readInnerText().trim().equals("true");
					else if (xml.event.text.equals("exclusions"))
						readExclusions(xml, dep.exclusions);
					else
						xml.closeElement();
				}
				dependencies.add(dep);
			} while (true);
		}

		private void readExclusions(XMLStreamReader xml, List<Pair<String, String>> exclusions) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing exclusions tag");
					break;
				}
				if (!xml.event.text.equals("exclusion")) {
					xml.closeElement();
					continue;
				}
				Pair<String, String> e = new Pair<>(null, null);
				ElementContext excluCtx = xml.event.context.getFirst();
				while (xml.nextInnerElement(excluCtx)) {
					if (xml.event.text.equals("groupId"))
						e.setValue1(xml.readInnerText().trim().asString());
					else if (xml.event.text.equals("artifactId"))
						e.setValue2(xml.readInnerText().trim().asString());
					else
						xml.closeElement();
				}
				if (e.getValue1().equals("*")) e.setValue1(null);
				if (e.getValue2().equals("*")) e.setValue2(null);
				exclusions.add(e);
			} while (true);
		}
		
		private void readProfiles(XMLStreamReader xml) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing profiles tag");
					break;
				}
				if (!xml.event.text.equals("profile")) {
					xml.closeElement();
					continue;
				}
				Profile profile = readProfile(xml);
				if (profile == null) continue;
				profiles.add(profile);
			} while (true);
		}

		private Profile readProfile(XMLStreamReader xml) throws Exception {
			if (xml.event.isClosed) return null;
			Profile profile = new Profile();
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing profile tag");
					break;
				}
				if (xml.event.text.equals("activation")) {
					ElementContext ctxActivation = xml.event.context.getFirst();
					while (xml.nextInnerElement(ctxActivation)) {
						if (xml.event.text.equals("activeByDefault")) {
							if (xml.readInnerText().trim().equals("true"))
								profile.activeByDefault = true;
						} else if (xml.event.text.equals("jdk")) {
							profile.jdk = xml.readInnerText().trim().asString();
						} else if (xml.event.text.equals("os")) {
							profile.activationOS = new ActivationOS();
							ElementContext ctxOS = xml.event.context.getFirst();
							while (xml.nextInnerElement(ctxOS)) {
								if (xml.event.text.equals("name"))
									profile.activationOS.name = xml.readInnerText().trim().asString();
								else if (xml.event.text.equals("family"))
									profile.activationOS.family = xml.readInnerText().trim().asString();
								else if (xml.event.text.equals("arch"))
									profile.activationOS.arch = xml.readInnerText().trim().asString();
								else if (xml.event.text.equals("version"))
									profile.activationOS.version = xml.readInnerText().trim().asString();
								else
									xml.closeElement();
							}
						} else if (xml.event.text.equals("property")) {
							ElementContext ctxProperty = xml.event.context.getFirst();
							while (xml.nextInnerElement(ctxProperty)) {
								if (xml.event.text.equals("name"))
									profile.activationPropertyName = xml.readInnerText().trim().asString();
								else if (xml.event.text.equals("value"))
									profile.activationPropertyValue = xml.readInnerText().trim().asString();
								else
									xml.closeElement();
							}
						} else if (xml.event.text.equals("file")) {
							ElementContext ctxFile = xml.event.context.getFirst();
							while (xml.nextInnerElement(ctxFile)) {
								if (xml.event.text.equals("missing"))
									profile.activationMissingFile = xml.readInnerText().trim().asString();
								else if (xml.event.text.equals("exists"))
									profile.activationFileExists = xml.readInnerText().trim().asString();
								else
									xml.closeElement();
							}
						} else
							xml.closeElement();
					}
				} else if (xml.event.text.equals("build"))
					readBuild(xml, profile.build);
				else if (xml.event.text.equals("properties"))
					readProperties(xml, profile.properties);
				else if (xml.event.text.equals("dependencyManagement"))
					readDependencyManagement(xml, profile.dependencyManagement);
				else if (xml.event.text.equals("dependencies"))
					readDependencies(xml, profile.dependencies);
				else if (xml.event.text.equals("repositories"))
					readRepositories(xml, profile.repositories);
				else
					xml.closeElement();
			} while (true);
			return profile;
		}

		private void readRepositories(XMLStreamReader xml, List<Repository> repositories) throws Exception {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing repositories tag");
					break;
				}
				if (!xml.event.text.equals("repository")) {
					xml.closeElement();
					continue;
				}
				Repository repo = readRepository(xml);
				if (repo != null)
					repositories.add(repo);
			} while (true);
		}
		
		private Repository readRepository(XMLStreamReader xml) throws Exception {
			if (xml.event.isClosed) return null;
			ElementContext ctx = xml.event.context.getFirst();
			Repository repo = new Repository();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new Exception("Invalid POM: missing closing repository tag");
					break;
				}
				if (xml.event.text.equals("id")) {
					// not used so far: repo.id = xml.readInnerText().trim().asString();
				} else if (xml.event.text.equals("url"))
					repo.url = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("releases")) {
					if (!xml.event.isClosed)
						do {
							if (!xml.nextInnerElement(ctx)) {
								if (!Type.END_ELEMENT.equals(xml.event.type))
									throw new Exception("Invalid POM: missing closing releases tag");
								break;
							}
							if (xml.event.text.equals("enabled")) {
								String s = xml.readInnerText().trim().asString();
								if (s.equalsIgnoreCase("false"))
									repo.releasesEnabled = false;
							}
						} while (true);
				} else if (xml.event.text.equals("snapshots")) {
					if (!xml.event.isClosed)
						do {
							if (!xml.nextInnerElement(ctx)) {
								if (!Type.END_ELEMENT.equals(xml.event.type))
									throw new Exception("Invalid POM: missing closing snapshots tag");
								break;
							}
							if (xml.event.text.equals("enabled")) {
								String s = xml.readInnerText().trim().asString();
								if (s.equalsIgnoreCase("false"))
									repo.snapshotsEnabled = false;
							}
						} while (true);
				} else
					xml.closeElement();
			} while (true);
			return repo;
		}
	}
	
	private class Finalize extends Task.Cpu<Void, NoException> {
		public Finalize(AsyncWork<MavenPOM, Exception> result, byte priority) {
			super("Finalize POM loading", priority);
			this.result = result;
		}
		
		private AsyncWork<MavenPOM, Exception> result;
		
		@Override
		public Void run() {
			Map<String, String> finalProperties = new HashMap<>();
			if (parentLoading != null) {
				if (parentLoading.hasError()) {
					result.error(new Exception("Error loading parent POM", parentLoading.getError()));
					return null;
				}
				if (parentLoading.isCancelled()) {
					result.cancel(parentLoading.getCancelEvent());
					return null;
				}
				MavenPOM parent = parentLoading.getResult();
				if (groupId == null) groupId = parent.groupId;
				if (version == null) version = parent.version;
				for (Map.Entry<String, String> p : parent.properties.entrySet()) {
					if (!properties.containsKey(p.getKey()))
						finalProperties.put(p.getKey(), p.getValue());
				}
				for (Dependency pdm : parent.dependencyManagement) {
					if (pdm.groupId == null) continue;
					if (pdm.artifactId == null) continue;
					boolean found = false;
					for (Dependency dm : dependencyManagement) {
						if (dm.groupId == null) continue;
						if (dm.artifactId == null) continue;
						if (!dm.groupId.equals(pdm.groupId)) continue;
						if (!dm.artifactId.equals(pdm.artifactId)) continue;
						found = true;
						break;
					}
					if (found) continue;
					dependencyManagement.add(pdm);
				}
			}
			resolveProperties(properties, finalProperties);
			
			Profile defaultProfile = null;
			Profile activeProfile = null;
			for (Profile profile : profiles) {
				if (checkProfile(profile, finalProperties)) {
					activeProfile = profile;
					break;
				}
				if (profile.activeByDefault && defaultProfile == null)
					defaultProfile = profile;
			}
			if (activeProfile == null) activeProfile = defaultProfile;
			if (activeProfile != null)
				addProfile(activeProfile);

			resolveProperties(properties, finalProperties);
			properties = finalProperties;
			
			// resolve elements
			
			groupId = resolve(groupId, finalProperties);
			artifactId = resolve(artifactId, finalProperties);
			version = resolve(version, finalProperties);
			if (packaging == null) packaging = "jar";
			
			build.outputDirectory = resolve(build.outputDirectory, finalProperties);
			
			resolveDependencies(dependencies, finalProperties);
			resolveDependencies(dependencyManagement, finalProperties);
			resolveRepositories(repositories, finalProperties);
			
			// handle dependencies management
			for (Dependency dep : dependencies) {
				if (dep.groupId == null) continue;
				if (dep.artifactId == null) continue;
				// TODO
				for (Dependency dm : dependencyManagement) {
					if (!dep.groupId.equals(dm.groupId)) continue;
					if (!dep.artifactId.equals(dm.artifactId)) continue;
					if (dm.version != null) dep.version = dm.version;
				}
			}
			
			result.unblockSuccess(MavenPOM.this);

			return null;
		}
		
		private void resolveDependencies(List<Dependency> deps, Map<String, String> props) {
			for (Dependency dep : deps) {
				dep.groupId = resolve(dep.groupId, props);
				dep.artifactId = resolve(dep.artifactId, props);
				dep.version = resolve(dep.version, props);
				dep.scope = resolve(dep.scope, props);
				dep.type = resolve(dep.type, props);
				dep.classifier = resolve(dep.classifier, props);
				dep.systemPath = resolve(dep.systemPath, props);
				for (Pair<String, String> e : dep.exclusions) {
					e.setValue1(resolve(e.getValue1(), props));
					e.setValue2(resolve(e.getValue2(), props));
				}
			}
		}
		
		private void resolveRepositories(List<Repository> repos, Map<String, String> props) {
			for (Repository repo : repos) {
				repo.url = resolve(repo.url, props);
			}
		}
		
		private void resolveProperties(Map<String, String> toResolve, Map<String, String> resolved) {
			while (!toResolve.isEmpty()) {
				boolean changed = false;
				for (Iterator<Map.Entry<String, String>> it = toResolve.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry<String, String> p = it.next();
					String value = resolve(p.getValue(), resolved);
					if (value == null) continue;
					changed = true;
					resolved.put(p.getKey(), value);
					it.remove();
				}
				if (!changed) break;
			}
		}
		
		private String resolve(String value, Map<String, String> properties) {
			if (value == null) return null;
			int i = value.indexOf("${");
			if (i < 0) return value;
			int j = value.indexOf('}', i + 2);
			if (j < 0) return value;
			String name = value.substring(i + 2, j).trim();
			if (properties.containsKey(name)) {
				StringBuffer s = new StringBuffer();
				if (i > 0) s.append(value.substring(0, i));
				s.append(properties.get(name));
				if (j < value.length() - 1) s.append(value.substring(j + 1));
				return resolve(s.toString(), properties);
			}
			if (name.equals("project.groupId")) {
				if (groupId != null) return groupId;
				return null;
			}
			if (name.equals("project.artifactId")) {
				if (artifactId != null) return artifactId;
				return null;
			}
			if (name.equals("project.version")) {
				if (version != null) return version;
				return null;
			}
			if (name.equals("parent.groupId")) {
				if (parentLoading != null) return parentLoading.getResult().groupId;
				return null;
			}
			if (name.equals("parent.artifactId")) {
				if (parentLoading != null) return parentLoading.getResult().artifactId;
				return null;
			}
			if (name.equals("parent.version")) {
				if (parentLoading != null) return parentLoading.getResult().version;
				return null;
			}
			if (name.startsWith("env.")) {
				return System.getenv(name.substring(4));
			}
			if (name.startsWith("settings.")) {
				// TODO
				return null;
			}
			return System.getProperty(name);
		}
	}
	
	private boolean checkProfile(Profile profile, Map<String, String> finalProperties) {
		if (profile.activationOS != null) {
			if (profile.activationOS.name != null) {
				if (!System.getProperty("os.name").toLowerCase(Locale.US).equals(profile.activationOS.name.toLowerCase()))
					return false;
			}
			if (profile.activationOS.arch != null) {
				if (!System.getProperty("os.arch").toLowerCase(Locale.US).equals(profile.activationOS.arch.toLowerCase()))
					return false;
			}
			if (profile.activationOS.family != null) {
				OSFamily family = SystemEnvironment.getOSFamily();
				if (family == null)
					return false;
				if (!profile.activationOS.family.toLowerCase().equals(family.getName()))
					return false;
			}
			if (profile.activationOS.version != null) {
				if (!System.getProperty("os.version").toLowerCase(Locale.US).equals(profile.activationOS.version.toLowerCase()))
					return false;
			}
		}
		if (profile.activationPropertyName != null) {
			if (profile.activationPropertyValue == null) {
				if (!properties.containsKey(profile.activationPropertyName) &&
					!finalProperties.containsKey(profile.activationPropertyName))
					return false;
			} else {
				String p1 = properties.get(profile.activationPropertyName);
				String p2 = finalProperties.get(profile.activationPropertyName);
				if (p1 == null) {
					if (p2 == null) return false;
					if (!p2.equals(profile.activationPropertyName)) return false;
				}
				if (!p1.equals(profile.activationPropertyName)) {
					if (p2 == null) return false;
					if (!p2.equals(profile.activationPropertyName)) return false;
				}
			}
		}
		if (profile.activationMissingFile != null) {
			// TODO
		}
		if (profile.activationFileExists != null) {
			// TODO
		}
		if (profile.jdk != null) {
			// TODO
		}
		return true;
	}
	
	private void addProfile(Profile profile) {
		if (profile.build.outputDirectory != null)
			build.outputDirectory = profile.build.outputDirectory;
		properties.putAll(profile.properties);
		dependencies.addAll(profile.dependencies);
		dependencyManagement.addAll(profile.dependencyManagement);
		repositories.addAll(profile.repositories);
	}

	/** Parse a version specification in POM format. */
	public static VersionSpecification parseVersionSpecification(String s) {
		if (s == null) return null;
		if (s.length() == 0) return null;
		char c = s.charAt(0);
		if (c == '[') {
			int i = s.indexOf(']');
			boolean excluded = false;
			if (i < 0) {
				i = s.indexOf(')');
				if (i > 0)
					excluded = true;
			}
			if (i < 0) {
				// no end ? consider a unique version
				return new VersionSpecification.SingleVersion(new Version(s));
			}
			String range = s.substring(1, i).trim();
			i = range.indexOf(',');
			if (i < 0) {
				// unique version
				return new VersionSpecification.SingleVersion(new Version(range));
			}
			Version min = new Version(range.substring(0, i).trim());
			range = range.substring(i + 1).trim();
			Version max;
			if (range.length() == 0 && excluded)
				max = null;
			else
				max = new Version(range);
			return new VersionSpecification.Range(new VersionRange(min, max, !excluded));
		}
		// TODO
		Version v = new Version(s);
		return new VersionSpecification.RangeWithRecommended(new VersionRange(v, null, false), v);
	}
	
}
