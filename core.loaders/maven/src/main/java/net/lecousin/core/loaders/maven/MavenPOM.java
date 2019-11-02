package net.lecousin.core.loaders.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionRange;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromURI;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Runnables;
import net.lecousin.framework.util.SystemEnvironment;
import net.lecousin.framework.util.SystemEnvironment.OSFamily;
import net.lecousin.framework.xml.XMLException;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;
import net.lecousin.framework.xml.XMLStreamReader;

/**
 * Implementation of LibraryDescriptor from a Maven POM file.
 */
public class MavenPOM implements LibraryDescriptor {
	
	public static final String ELEMENT_ARTIFACT_ID = "artifactId";
	public static final String ELEMENT_GROUP_ID = "groupId";
	public static final String ELEMENT_VERSION = "version";
	public static final String ELEMENT_DEPENDENCIES = "dependencies";
	
	/** Load a POM file. */
	public static AsyncSupplier<MavenPOM, LibraryManagementException> load(
		URI pomFile, byte priority, MavenPOMLoader pomLoader, boolean fromRepository
	) {
		AsyncSupplier<MavenPOM, LibraryManagementException> result = new AsyncSupplier<>();
		IOProvider p = IOProviderFromURI.getInstance().get(pomFile);
		if (p == null)
			return new AsyncSupplier<>(null,
				new MavenPOMException(pomFile, new FileNotFoundException(pomFile.toString())));
		if (!(p instanceof IOProvider.Readable))
			return new AsyncSupplier<>(null, new MavenPOMException(pomFile, "not readable"));
		IO.Readable fileIO;
		try { fileIO = ((IOProvider.Readable)p).provideIOReadable(priority); }
		catch (IOException e) {
			return new AsyncSupplier<>(null, new MavenPOMException(pomFile, e));
		}
		Task<Void, NoException> task = new Task.Cpu<Void, NoException>("Loading POM", priority) {
			@SuppressWarnings("unchecked")
			@Override
			public Void run() {
				AsyncSupplier<? extends IO.Readable.Buffered, IOException> readFile;
				int bufSize;
				if (fileIO instanceof IO.KnownSize) {
					try { bufSize = (int)((IO.KnownSize)fileIO).getSizeSync(); }
					catch (IOException e) {
						result.error(new MavenPOMException(pomFile, e));
						return null;
					}
					byte[] buf = new byte[bufSize];
					readFile = new AsyncSupplier<>();
					fileIO.readFullyAsync(ByteBuffer.wrap(buf)).onDone(
						() -> ((AsyncSupplier<ByteArrayIO, IOException>)readFile).unblockSuccess(
							new ByteArrayIO(buf, pomFile.toString())),
						readFile);
				} else {
					bufSize = 4096;
					readFile = IOUtil.readFullyAsync(fileIO, 4096);
				}
				MavenPOM pom = new MavenPOM(pomLoader, pomFile);
				readFile.thenStart(new Task.Cpu.FromRunnable("Loading POM", priority, () -> {
					fileIO.closeAsync();
					if (readFile.hasError()) {
						result.error(new MavenPOMException(pomFile, readFile.getError()));
						return;
					}
					IO.Readable.Buffered bio = readFile.getResult();
					AsyncSupplier<XMLStreamReader, Exception> startXMLReader = XMLStreamReader.start(bio, bufSize, 3, false);
					Reader read = pom.new Reader(startXMLReader, priority, fromRepository, pomFile, pomLoader);
					read.startOn(startXMLReader, true);
					read.getOutput().onDone(() -> {
						if (pom.parentLoading == null) {
							pom.new Finalize(result, priority).start();
							return;
						}
						pom.parentLoading.onDone(() -> pom.new Finalize(result, priority).start(), result);
					}, result);
				}), true);
				return null;
			}
		};
		task.startOn(fileIO.canStartReading(), true);
		return result;
	}
	
	private MavenPOM(MavenPOMLoader loader, URI pomFile) {
		this.loader = loader;
		this.pomFile = pomFile;
	}
	
	private MavenPOMLoader loader;
	private URI pomFile;
	
	private String artifactId;
	private String groupId;
	private String version;
	private String packaging;
	
	private String parentArtifactId;
	private String parentGroupId;
	private String parentVersion;
	private String parentRelativePath = "../pom.xml";
	private AsyncSupplier<MavenPOM, LibraryManagementException> parentLoading;
	
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
		public URI getKnownLocation() {
			if (systemPath == null) return null;
			try {
				URI uri = new URI(systemPath);
				if (uri.isAbsolute())
					return uri;
				URI parent = pomFile.resolve(".");
				return parent.resolve(systemPath); 
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
	public URI getDirectory() {
		try {
			return pomFile.resolve(".");
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public boolean hasClasses() {
		return "jar".equals(packaging) || "bundle".equals(packaging);
	}
	
	private File classesFile = null;
	
	@Override
	public AsyncSupplier<File, NoException> getClasses() {
		if (!hasClasses()) return new AsyncSupplier<>(null, null);
		if (classesFile != null) return new AsyncSupplier<>(classesFile, null);
		if ("file".equals(pomFile.getScheme())) {
			File pf = new File(pomFile);
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
			return new AsyncSupplier<>(classesFile, null);
		}
		try {
			URI uri = pomFile.resolve(".").resolve(artifactId + '-' + version + ".jar");
			IOProvider p = IOProviderFromURI.getInstance().get(uri);
			if (!(p instanceof IOProvider.Readable))
				return new AsyncSupplier<>(null, null);
			IO.Readable io = ((IOProvider.Readable)p).provideIOReadable(Task.PRIORITY_IMPORTANT);
			if (io == null)
				return new AsyncSupplier<>(null, null);
			AsyncSupplier<File, IOException> download = IOUtil.toTempFile(io);
			AsyncSupplier<File, NoException> result = new AsyncSupplier<>();
			download.onDone(() -> {
				if (download.isSuccessful()) {
					classesFile = download.getResult();
					result.unblockSuccess(classesFile);
				} else {
					result.unblockSuccess(null);
				}
				
			});
			return result;
		} catch (Exception e) {
			return new AsyncSupplier<>(null, null);
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
	
	/** Returns all declared dependencies (getDependencies only return 'compile' dependencies). */
	public List<LibraryDescriptor.Dependency> getAllDependenciesAnyScope() {
		ArrayList<LibraryDescriptor.Dependency> list = new ArrayList<>(dependencies.size());
		list.addAll(dependencies);
		return list;
	}
	
	/** Return the file name for an artifact. */
	public static String getFilename(String artifactId, String version, String classifier, String type) {
		String cl = classifier == null || classifier.isEmpty() ? null : classifier;
		String extension = "jar";
		if (type != null) {
			switch (type) {
			case "test-jar":
				if (cl == null) cl = "tests";
				break;
			case "maven-plugin":
			case "ejb":
				break; // extension .jar
			case "ejb-client":
				if (cl == null) cl = "client";
				break;
			case "java-source":
				if (cl == null) cl = "sources";
				break;
			case "javadoc":
				if (cl == null) cl = "javadoc";
				break;
			default:
				extension = type;
				break;
			}
		}
		StringBuilder name = new StringBuilder(100);
		name.append(artifactId).append('-').append(version);
		if (cl != null)
			name.append('-').append(cl);
		name.append('.').append(extension);
		return name.toString();
	}

	private class Reader extends Task.Cpu<Void, LibraryManagementException> {
		private Reader(AsyncSupplier<XMLStreamReader, Exception> startXMLReader, byte priority,
			boolean fromRepository, URI pomFile, MavenPOMLoader pomLoader) {
			super("Read POM " + pomFile.toString(), priority);
			this.startXMLReader = startXMLReader;
			this.fromRepository = fromRepository;
			this.pomFile = pomFile;
			this.pomLoader = pomLoader;
		}
		
		private AsyncSupplier<XMLStreamReader, Exception> startXMLReader;
		private boolean fromRepository;
		private URI pomFile;
		private MavenPOMLoader pomLoader;
		
		@Override
		@SuppressWarnings("squid:S3776") // complexity
		public Void run() throws LibraryManagementException {
			if (startXMLReader.hasError()) throw new MavenPOMException(pomFile, startXMLReader.getError());
			if (startXMLReader.isCancelled()) throw new MavenPOMException(pomFile, startXMLReader.getCancelEvent());
			XMLStreamReader xml = startXMLReader.getResult();
			try {
				while (!Type.START_ELEMENT.equals(xml.event.type)) xml.next();
				if (!xml.event.text.equals("project"))
					throw new MavenPOMException(pomFile, "Invalid POM: root element must be a project");
				while (xml.nextStartElement()) {
					if (xml.event.text.equals(ELEMENT_ARTIFACT_ID))
						artifactId = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals(ELEMENT_GROUP_ID))
						groupId = xml.readInnerText().trim().asString();
					else if (xml.event.text.equals(ELEMENT_VERSION))
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
					else if (xml.event.text.equals(ELEMENT_DEPENDENCIES))
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
						File parentFile = new File(new File(pomFile).getParentFile(), parentRelativePath);
						if (parentFile.exists()) {
							if (parentFile.isDirectory()) {
								parentFile = new File(parentFile, "pom.xml");
								if (parentFile.exists())
									parentLoading = pomLoader.loadPOM(parentFile.toURI(), false, getPriority());
							} else {
								parentLoading = pomLoader.loadPOM(parentFile.toURI(), false, getPriority());
							}
						}
					}
					if (parentLoading == null) {
						// TODO try to resolve properties ?
						parentLoading = pomLoader.loadLibrary(
							parentGroupId, parentArtifactId,
							new VersionSpecification.SingleVersion(new Version(parentVersion)),
							getPriority(), getRepositories(repositories));
					}
				}
				return null;
			} catch (LibraryManagementException e) {
				throw e;
			} catch (Exception e) {
				throw new MavenPOMException(pomFile, e);
			}
		}
		
		private void readInnerElements(XMLStreamReader xml, Runnables.Throws<Exception> onInnerElement)
		throws MavenPOMException, XMLException, IOException {
			if (xml.event.isClosed) return;
			ElementContext ctx = xml.event.context.getFirst();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new MavenPOMException(pomFile, "Invalid POM: missing closing tag for " + ctx.text.toString());
					return;
				}
				try {
					onInnerElement.run();
				} catch (MavenPOMException | XMLException | IOException e) {
					throw e;
				} catch (Exception e) {
					throw new MavenPOMException("Error reading " + ctx.text.toString(), e);
				}
			} while (true);
			
		}
		
		private void readParent(XMLStreamReader xml) throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals(ELEMENT_ARTIFACT_ID))
					parentArtifactId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals(ELEMENT_GROUP_ID))
					parentGroupId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals(ELEMENT_VERSION))
					parentVersion = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("relativePath"))
					parentRelativePath = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			});
		}

		private void readBuild(XMLStreamReader xml, Build build) throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("outputDirectory"))
					build.outputDirectory = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			});
		}

		private void readProperties(XMLStreamReader xml, Map<String, String> properties) throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> properties.put(xml.event.text.asString(), xml.readInnerText().asString()));
		}

		private void readDependencyManagement(XMLStreamReader xml, List<Dependency> dependencyManagement)
		throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals(ELEMENT_DEPENDENCIES))
					readDependencies(xml, dependencyManagement);
				else
					xml.closeElement();
			});
		}

		private void readDependencies(XMLStreamReader xml, List<Dependency> dependencies)
		throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("dependency"))
					readDependency(xml, dependencies);
				else
					xml.closeElement();
			});
		}
		
		private void readDependency(XMLStreamReader xml, List<Dependency> dependencies)
		throws MavenPOMException, XMLException, IOException {
			Dependency dep = new Dependency();
			readInnerElements(xml, () -> {
				if (xml.event.text.equals(ELEMENT_GROUP_ID))
					dep.groupId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals(ELEMENT_ARTIFACT_ID))
					dep.artifactId = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals(ELEMENT_VERSION))
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
			});
			dependencies.add(dep);
		}

		private void readExclusions(XMLStreamReader xml, List<Pair<String, String>> exclusions)
		throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("exclusion")) {
					Pair<String, String> e = new Pair<>(null, null);
					ElementContext excluCtx = xml.event.context.getFirst();
					while (xml.nextInnerElement(excluCtx)) {
						if (xml.event.text.equals(ELEMENT_GROUP_ID))
							e.setValue1(xml.readInnerText().trim().asString());
						else if (xml.event.text.equals(ELEMENT_ARTIFACT_ID))
							e.setValue2(xml.readInnerText().trim().asString());
						else
							xml.closeElement();
					}
					if ("*".equals(e.getValue1())) e.setValue1(null);
					if ("*".equals(e.getValue2())) e.setValue2(null);
					exclusions.add(e);
				} else {
					xml.closeElement();
				}
			});
		}
		
		private void readProfiles(XMLStreamReader xml) throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("profile")) {
					Profile profile = readProfile(xml);
					if (profile != null)
						profiles.add(profile);
				} else {
					xml.closeElement();
				}
			});
		}

		private Profile readProfile(XMLStreamReader xml) throws MavenPOMException, XMLException, IOException {
			Profile profile = new Profile();
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("activation"))
					readProfileActivation(xml, profile);
				else if (xml.event.text.equals("build"))
					readBuild(xml, profile.build);
				else if (xml.event.text.equals("properties"))
					readProperties(xml, profile.properties);
				else if (xml.event.text.equals("dependencyManagement"))
					readDependencyManagement(xml, profile.dependencyManagement);
				else if (xml.event.text.equals(ELEMENT_DEPENDENCIES))
					readDependencies(xml, profile.dependencies);
				else if (xml.event.text.equals("repositories"))
					readRepositories(xml, profile.repositories);
				else
					xml.closeElement();
			});
			return profile;
		}
		
		private void readProfileActivation(XMLStreamReader xml, Profile profile) throws XMLException, IOException, MavenPOMException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("activeByDefault"))
					readProfileActivationActiveByDefault(xml, profile);
				else if (xml.event.text.equals("jdk"))
					profile.jdk = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("os"))
					readProfileActivationOS(xml, profile);
				else if (xml.event.text.equals("property"))
					readProfileActivationProperty(xml, profile);
				else if (xml.event.text.equals("file"))
					readProfileActivationFile(xml, profile);
				else
					xml.closeElement();
			});
		}
		
		private void readProfileActivationActiveByDefault(XMLStreamReader xml, Profile profile) throws XMLException, IOException {
			if (xml.readInnerText().trim().equals("true"))
				profile.activeByDefault = true;
		}
		
		private void readProfileActivationOS(XMLStreamReader xml, Profile profile) throws XMLException, IOException, MavenPOMException {
			profile.activationOS = new ActivationOS();
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("name"))
					profile.activationOS.name = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("family"))
					profile.activationOS.family = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("arch"))
					profile.activationOS.arch = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals(ELEMENT_VERSION))
					profile.activationOS.version = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			});
		}
		
		private void readProfileActivationProperty(XMLStreamReader xml, Profile profile) throws XMLException, IOException, MavenPOMException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("name"))
					profile.activationPropertyName = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("value"))
					profile.activationPropertyValue = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			});
		}
		
		private void readProfileActivationFile(XMLStreamReader xml, Profile profile) throws XMLException, IOException, MavenPOMException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("missing"))
					profile.activationMissingFile = xml.readInnerText().trim().asString();
				else if (xml.event.text.equals("exists"))
					profile.activationFileExists = xml.readInnerText().trim().asString();
				else
					xml.closeElement();
			});
		}

		private void readRepositories(XMLStreamReader xml, List<Repository> repositories)
		throws MavenPOMException, XMLException, IOException {
			readInnerElements(xml, () -> {
				if (xml.event.text.equals("repository")) {
					Repository repo = readRepository(xml);
					if (repo != null)
						repositories.add(repo);
				} else {
					xml.closeElement();
				}
			});
		}
		
		private Repository readRepository(XMLStreamReader xml) throws MavenPOMException, XMLException, IOException {
			if (xml.event.isClosed) return null;
			ElementContext ctx = xml.event.context.getFirst();
			Repository repo = new Repository();
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new MavenPOMException(pomFile, "Invalid POM: missing closing repository tag");
					break;
				}
				if (xml.event.text.equals("id")) {
					// not used so far: repo.id = xml.readInnerText().trim().asString();
				} else if (xml.event.text.equals("url")) {
					repo.url = xml.readInnerText().trim().asString();
				} else if (xml.event.text.equals("releases")) {
					readRepositoryReleases(xml, ctx, repo);
				} else if (xml.event.text.equals("snapshots")) {
					readRepositorySnapshots(xml, ctx, repo);
				} else {
					xml.closeElement();
				}
			} while (true);
			return repo;
		}
		
		private void readRepositoryReleases(XMLStreamReader xml, ElementContext ctx, Repository repo)
		throws XMLException, MavenPOMException, IOException {
			if (xml.event.isClosed)
				return;
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new MavenPOMException(pomFile,
							"Invalid POM: missing closing releases tag");
					break;
				}
				if (xml.event.text.equals("enabled")) {
					String s = xml.readInnerText().trim().asString();
					if (s.equalsIgnoreCase("false"))
						repo.releasesEnabled = false;
				}
			} while (true);
		}

		private void readRepositorySnapshots(XMLStreamReader xml, ElementContext ctx, Repository repo)
		throws XMLException, MavenPOMException, IOException {
			if (xml.event.isClosed) return;
			do {
				if (!xml.nextInnerElement(ctx)) {
					if (!Type.END_ELEMENT.equals(xml.event.type))
						throw new MavenPOMException(pomFile,
							"Invalid POM: missing closing snapshots tag");
					break;
				}
				if (xml.event.text.equals("enabled")) {
					String s = xml.readInnerText().trim().asString();
					if (s.equalsIgnoreCase("false"))
						repo.snapshotsEnabled = false;
				}
			} while (true);
		}
	}
	
	private class Finalize extends Task.Cpu<Void, NoException> {
		public Finalize(AsyncSupplier<MavenPOM, LibraryManagementException> result, byte priority) {
			super("Finalize POM loading", priority);
			this.finalResult = result;
		}
		
		private AsyncSupplier<MavenPOM, LibraryManagementException> finalResult;
		
		@Override
		public Void run() {
			Map<String, String> finalProperties = new HashMap<>();
			if (parentLoading != null) {
				if (parentLoading.hasError()) {
					finalResult.error(new MavenPOMException("Error loading parent POM", parentLoading.getError()));
					return null;
				}
				if (parentLoading.isCancelled()) {
					finalResult.cancel(parentLoading.getCancelEvent());
					return null;
				}
				inheritFromParent(parentLoading.getResult(), finalProperties);
			}
			resolveProperties(properties, finalProperties);
			
			List<Profile> activeProfiles = getActiveProfiles(finalProperties);
			for (Profile p : activeProfiles)
				addProfile(p);

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
			
			finalResult.unblockSuccess(MavenPOM.this);

			return null;
		}
		
		private void inheritFromParent(MavenPOM parent, Map<String, String> finalProperties) {
			if (groupId == null) groupId = parent.groupId;
			if (version == null) version = parent.version;
			for (Map.Entry<String, String> p : parent.properties.entrySet()) {
				if (!properties.containsKey(p.getKey()))
					finalProperties.put(p.getKey(), p.getValue());
			}
			for (Dependency pdm : parent.dependencyManagement) {
				if (pdm.groupId == null || pdm.artifactId == null) continue;
				boolean found = false;
				for (Dependency dm : dependencyManagement)
					if (dm.groupId != null &&
						dm.artifactId != null &&
						dm.groupId.equals(pdm.groupId) &&
						dm.artifactId.equals(pdm.artifactId)) {
						found = true;
						break;
					}
				if (!found)
					dependencyManagement.add(pdm);
			}
		}
		
		private List<Profile> getActiveProfiles(Map<String, String> finalProperties) {
			List<Profile> defaultProfiles = new LinkedList<>();
			List<Profile> activeProfiles = new LinkedList<>();
			for (Profile profile : profiles) {
				if (checkProfile(profile, finalProperties))
					activeProfiles.add(profile);
				else if (profile.activeByDefault)
					defaultProfiles.add(profile);
			}
			if (!activeProfiles.isEmpty())
				return activeProfiles;
			return defaultProfiles;

		}
		
		private boolean checkProfile(Profile profile, Map<String, String> finalProperties) {
			if (profile.activationOS != null && !checkProfileOS(profile.activationOS))
				return false;
			if (profile.activationPropertyName != null && !checkProfileProperties(profile, finalProperties))
				return false;
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
		
		private boolean checkProfileOS(ActivationOS os) {
			if (os.family != null) {
				OSFamily family = SystemEnvironment.getOSFamily();
				if (family == null)
					return false;
				if (!checkProfileCondition(os.family.toLowerCase(), family.getName()))
					return false;
			}
			return
				checkProfileCondition(os.name, System.getProperty("os.name")) &&
				checkProfileCondition(os.arch, System.getProperty("os.arch")) &&
				checkProfileCondition(os.version, System.getProperty("os.version"));
		}
		
		private boolean checkProfileProperties(Profile profile, Map<String, String> finalProperties) {
			if (profile.activationPropertyValue == null) {
				String s = profile.activationPropertyName;
				boolean presentExpected = !s.startsWith("!");
				if (!presentExpected) s = s.substring(1);
				if (presentExpected != properties.containsKey(s) &&
					presentExpected != finalProperties.containsKey(s))
					return false;
			} else {
				String p1 = properties.get(profile.activationPropertyName);
				String p2 = finalProperties.get(profile.activationPropertyName);
				String value = profile.activationPropertyName;
				boolean presentExpected = !value.startsWith("!");
				if (!presentExpected) value = value.substring(1);
				if (p1 == null || presentExpected != p1.equals(value)) {
					if (p2 == null) return false;
					if (presentExpected != p2.equals(value)) return false;
				}
			}
			return true;
		}
		
		private boolean checkProfileCondition(String condition, String value) {
			if (condition == null)
				return true;
			if (condition.startsWith("!"))
				return !checkProfileCondition(condition.substring(1), value);
			return value != null && value.equalsIgnoreCase(condition);
		}
		
		private void addProfile(Profile profile) {
			if (profile.build.outputDirectory != null)
				build.outputDirectory = profile.build.outputDirectory;
			properties.putAll(profile.properties);
			dependencies.addAll(profile.dependencies);
			dependencyManagement.addAll(profile.dependencyManagement);
			repositories.addAll(profile.repositories);
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
				StringBuilder s = new StringBuilder();
				if (i > 0) s.append(value.substring(0, i));
				s.append(properties.get(name));
				if (j < value.length() - 1) s.append(value.substring(j + 1));
				return resolve(s.toString(), properties);
			}
			if (name.equals("project.groupId"))
				return groupId;
			if (name.equals("project.artifactId"))
				return artifactId;
			if (name.equals("project.version"))
				return version;
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
	
	@Override
	public String toString() {
		return getGroupId() + ':' + getArtifactId() + ':' + getVersionString();
	}
	
}
