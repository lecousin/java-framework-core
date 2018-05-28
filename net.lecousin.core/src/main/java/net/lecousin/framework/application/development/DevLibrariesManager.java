package net.lecousin.framework.application.development;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationBootstrap;
import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.SplashScreen;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.artifacts.LoadedLibrary;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor.Dependency;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader.DependencyNode;
import net.lecousin.framework.application.libraries.classloader.AbstractClassLoader;
import net.lecousin.framework.application.libraries.classloader.AppClassLoader;
import net.lecousin.framework.application.libraries.artifacts.ArtifactsLibrariesManager;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.application.libraries.classpath.DefaultApplicationClassLoader;
import net.lecousin.framework.application.libraries.classpath.LoadLibraryExtensionPointsFile;
import net.lecousin.framework.application.libraries.classpath.LoadLibraryPluginsFile;
import net.lecousin.framework.collections.TreeWithParent;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.FullReadFileTask;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.provider.IOProviderFromName;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.progress.FakeWorkProgress;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;

public class DevLibrariesManager implements ArtifactsLibrariesManager {

	public DevLibrariesManager(ArrayList<File> devPaths, SplashScreen splash, List<LibraryDescriptorLoader> loaders, File appDir, ApplicationConfiguration cfg, List<Triple<String, String, String>> plugins) {
		this.devPaths = devPaths;
		this.splash = splash;
		this.loaders = loaders;
		this.appCfg = cfg;
		this.loadPlugins = plugins;
		this.appDir = appDir;
	}
	
	private Application app;
	private File appDir;
	private AppClassLoader appClassLoader;
	private DefaultApplicationClassLoader defaultClassLoader;
	private ArrayList<File> devPaths;
	private SplashScreen splash;
	private List<LibraryDescriptorLoader> loaders;
	private JoinPoint<Exception> canStartApp = new JoinPoint<>();
	private Lib appLib;
	private ApplicationConfiguration appCfg;
	private List<Triple<String, String, String>> loadPlugins;
	
	private static class Lib {
		private LibraryDescriptor descr;
		private SynchronizationPoint<Exception> load = new SynchronizationPoint<>();
		private LoadedLibrary library;
	}
	
	private Map<String, Lib> libraries = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
	public DefaultApplicationClassLoader start(Application app) {
		this.app = app;
		this.appClassLoader = new AppClassLoader(app);
		this.defaultClassLoader = new DefaultApplicationClassLoader(app, null);
		
		app.getDefaultLogger().debug("Start loading application in development mode");
		
		long work = splash != null ? splash.getRemainingWork() : 0;
		work = work - work * 40 / 100; // loading is 60%, 40% will be application startup
		long stepDevProjects = work / 20; // 5%
		long stepDependencies = work * 80 / 100; // 80%;
		long stepVersionConflicts = work - stepDevProjects - stepDependencies; // 15%
		// TODO LoadLibrary should make progress
		
		if (splash != null) {
			if (appCfg.splash != null) {
				File splashFile = new File(appDir, appCfg.splash);
				if (splashFile.exists()) {
					FullReadFileTask read = new FullReadFileTask(splashFile, Task.PRIORITY_URGENT);
					read.start();
					Task<Void,NoException> load = new Task.Cpu<Void,NoException>("Loading splash image", Task.PRIORITY_URGENT) {
						@Override
						public Void run() {
							ImageIcon img = new ImageIcon(read.getResult());
							if (splash == null) return null;
							synchronized (splash) {
								if (!splash.isReady())
									try { splash.wait(); }
									catch (InterruptedException e) { return null; }
							}
							splash.setLogo(img, true);
							return null;
						}
					};
					read.ondone(load, false);
				} else
					splash.loadDefaultLogo();
			} else
				splash.loadDefaultLogo();
			splash.setText("Analyzing development projects");
		}
		
		// load dev projects
		JoinPoint<Exception> jpDevProjects = new JoinPoint<>();
		ArrayList<AsyncWork<? extends LibraryDescriptor, Exception>> devProjects = new ArrayList<>(devPaths.size());
		Task.Cpu<Void, NoException> loadDevProjects = new Task.Cpu<Void, NoException>("Load development projects", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				int nb = devPaths.size();
				long w = stepDevProjects;
				for (File dir : devPaths) {
					long step = w/nb--;
					w -= step;
					AsyncWork<? extends LibraryDescriptor, Exception> load = null;
					for (LibraryDescriptorLoader loader : loaders)
						if (loader.detect(dir)) {
							load = loader.loadProject(dir, Task.PRIORITY_IMPORTANT);
							break;
						}
					if (load == null) {
						app.getDefaultLogger().error("Unknown type of project: " + dir.getAbsolutePath());
						if (splash != null) splash.progress(step);
						continue;
					}
					if (splash != null)
						load.listenInline(new Runnable() {
							@Override
							public void run() {
								splash.progress(step);
							}
						});
					devProjects.add(load);
					jpDevProjects.addToJoin(load);
				}
				jpDevProjects.start();
				// free memory
				devPaths = null;
				return null;
			}
		};
		loadDevProjects.start();
		// get application project
		jpDevProjects.listenAsync(new Task.Cpu<Void, NoException>("Load application libraries", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				if (jpDevProjects.hasError()) { canStartApp.error(jpDevProjects.getError()); return null; }
				LibraryDescriptor appLib = null;
				for (AsyncWork<? extends LibraryDescriptor, Exception> p : devProjects) {
					LibraryDescriptor lib = p.getResult();
					if (lib == null) continue;
					if (!app.getGroupId().equals(lib.getGroupId())) continue;
					if (!app.getArtifactId().equals(lib.getArtifactId())) continue;
					appLib = lib;
					break;
				}
				if (appLib == null) {
					canStartApp.error(new Exception("Cannot find application " + app.getGroupId() + ':' + app.getArtifactId()));
					return null;
				}
				if (!appLib.hasClasses()) {
					canStartApp.error(new Exception("Application project must provide classes"));
					return null;
				}
				
				app.getDefaultLogger().debug("Development projects analyzed, loading application configuration file");
				
				List<LibraryDescriptor> addPlugins = new LinkedList<>();
				for (Triple<String, String, String> t : loadPlugins) {
					for (AsyncWork<? extends LibraryDescriptor, Exception> p : devProjects) {
						LibraryDescriptor lib = p.getResult();
						if (!lib.getGroupId().equals(t.getValue1())) continue;
						if (t.getValue2() != null && !lib.getArtifactId().equals(t.getValue2())) continue;
						if (t.getValue3() != null && !lib.getVersionString().equals(t.getValue3())) continue;
						addPlugins.add(lib);
					}
				}
				
				// load library
				loadApplicationLibrary(appLib, addPlugins, stepDependencies, stepVersionConflicts);
				return null;
			}
		}, true);
		return defaultClassLoader;
	}
	
	private void loadApplicationLibrary(LibraryDescriptor descr, List<LibraryDescriptor> addPlugins, long stepDependencies, long stepVersionConflicts) {
		app.getDefaultLogger().debug("Building dependencies tree");
		if (splash != null) splash.setText("Analyzing dependencies");

		TreeWithParent<DependencyNode> tree = new TreeWithParent<>(null);
		Map<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> artifacts = new HashMap<>();
		JoinPoint<NoException> treeDone = new JoinPoint<>();
		buildDependenciesTree(descr, tree, artifacts, new ArrayList<>(0), treeDone, addPlugins, splash, stepDependencies);
		treeDone.start();
		ResolveVersionConflicts resolveConflicts = new ResolveVersionConflicts(artifacts, descr.getLoader(), splash, stepVersionConflicts);
		resolveConflicts.startOn(treeDone, true);
		resolveConflicts.getOutput().listenInline(new Runnable() {
			@Override
			public void run() {
				if (resolveConflicts.hasError()) { canStartApp.error(resolveConflicts.getError()); return; }

				app.getDefaultLogger().debug("Dependencies analyzed, loading and initializing libraries");
				
				if (splash != null) splash.setText("Initializing libraries");

				Lib lib = new Lib();
				lib.descr = descr;
				libraries.put(descr.getGroupId() + ':' + descr.getArtifactId(), lib);
				appLib = lib;
				new LoadLibrary(lib, resolveConflicts.getResult(), addPlugins).start();;
				lib.load.listenAsync(new Task.Cpu<Void, NoException>("Finishing to initialize", Task.PRIORITY_IMPORTANT) {
					@Override
					public Void run() {
						if (canStartApp.hasError()) return null;
						if (lib.load.hasError()) { canStartApp.error(lib.load.getError()); return null; }
						app.getDefaultLogger().debug("Libraries initialized.");
						ExtensionPoints.allPluginsLoaded();
						canStartApp.unblock();
						return null;
					}
				}, true);
			}
		});
	}
	
	private void buildDependenciesTree(LibraryDescriptor descr, TreeWithParent<DependencyNode> tree, Map<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> artifacts, List<Pair<String, String>> exclusions, JoinPoint<NoException> jp, List<LibraryDescriptor> addPlugins, WorkProgress progress, long work) {
		// TODO keep optional that were not found to avoid searching again and again
		List<LibraryDescriptor.Dependency> deps = descr.getDependencies();
		if (addPlugins != null) {
			ArrayList<LibraryDescriptor.Dependency> newDeps = new ArrayList<>(deps.size() + addPlugins.size());
			newDeps.addAll(deps);
			for (LibraryDescriptor l : addPlugins)
				newDeps.add(new LibraryDescriptor.Dependency() {
					@Override
					public String getGroupId() { return l.getGroupId(); }
					@Override
					public String getArtifactId() { return l.getArtifactId(); }
					@Override
					public VersionSpecification getVersionSpecification() { return new VersionSpecification.SingleVersion(l.getVersion()); }
					@Override
					public String getClassifier() { return null; }
					@Override
					public boolean isOptional() { return false; }
					@Override
					public File getKnownLocation() { return l.getDirectory(); }
					@Override
					public List<Pair<String, String>> getExcludedDependencies() { return new ArrayList<>(0); }
				});
			deps = newDeps;
		}
		int nb = deps.size();
		for (LibraryDescriptor.Dependency dep : deps) {
			if (isMatching(dep.getGroupId(), dep.getArtifactId(), exclusions)) {
				nb--;
				continue;
			}
			app.getDefaultLogger().debug("Dependency: " + descr.getGroupId() + ':' + descr.getArtifactId() + ':' + descr.getVersionString() + " => " + dep.getGroupId() + ':' + dep.getArtifactId() + ':' + dep.getVersionSpecification());
			long step = work/nb--;
			work -= step;
			
			DependencyNode node = new DependencyNode();
			node.dep = dep;
			if (dep.getGroupId() == null || dep.getGroupId().length() == 0)
				node.descr = new AsyncWork<>(null, new Exception("Missing groupId in dependency"));
			else if (dep.getArtifactId() == null || dep.getArtifactId().length() == 0)
				node.descr = new AsyncWork<>(null, new Exception("Missing artifactId in dependency"));
			else {
				VersionSpecification depV = dep.getVersionSpecification();
				if (depV == null)
					node.descr = new AsyncWork<>(null, new Exception("Missing version in dependency"));
				else
					node.descr = descr.getLoader().loadLibrary(dep.getGroupId(), dep.getArtifactId(), depV, Task.PRIORITY_RATHER_IMPORTANT);
			}
			TreeWithParent.Node<DependencyNode> n;
			synchronized (tree) { n = tree.add(node); }
			List<TreeWithParent.Node<DependencyNode>> l;
			synchronized (artifacts) {
				Map<String, List<TreeWithParent.Node<DependencyNode>>> group = artifacts.get(dep.getGroupId());
				if (group == null) {
					group = new HashMap<>();
					artifacts.put(dep.getGroupId(), group);
				}
				List<TreeWithParent.Node<DependencyNode>> list = group.get(dep.getArtifactId());
				if (list == null) {
					list = new LinkedList<>();
					group.put(dep.getArtifactId(), list);
				}
				list.add(n);
				l = list;
			}
			jp.addToJoin(1);
			node.descr.listenInline(new Runnable() {
				@Override
				public void run() {
					if (node.descr.getResult() != null) {
						List<Pair<String, String>> addExcl = dep.getExcludedDependencies();
						ArrayList<Pair<String, String>> excl = new ArrayList<>(exclusions.size() + addExcl.size());
						excl.addAll(exclusions);
						for (Pair<String, String> e : addExcl)
							if (!excl.contains(e))
								excl.add(e);
						if (progress != null) progress.progress(step / 2);
						buildDependenciesTree(node.descr.getResult(), n.getSubNodes(), artifacts, excl, jp, null, progress, step - step / 2);
					} else {
						if (progress != null) progress.progress(step);
						if (dep.isOptional()) {
							// optional => no error
							app.getDefaultLogger().debug("Dependency " + dep.getGroupId() + ':' + dep.getArtifactId() + ':' + dep.getVersionSpecification() + " not found, but optional");
							synchronized (tree) {
								tree.removeInstance(node);
							}
							synchronized (artifacts) {
								l.remove(n);
								if (l.isEmpty()) {
									Map<String, List<TreeWithParent.Node<DependencyNode>>> group = artifacts.get(dep.getGroupId());
									group.remove(dep.getArtifactId());
									if (group.isEmpty())
										artifacts.remove(dep.getGroupId());
								}
							}
						}
					}
					jp.joined();
				}
			});
		}
		if (progress != null && work > 0) progress.progress(work);
	}
	
	private static boolean isMatching(String groupId, String artifactId, List<Pair<String, String>> list) {
		for (Pair<String, String> p : list) {
			if (p.getValue1() != null && !p.getValue1().equals(groupId)) continue;
			if (p.getValue2() != null && !p.getValue2().equals(artifactId)) continue;
			return true;
		}
		return false;
	}
	
	private class ResolveVersionConflicts extends Task.Cpu<Map<String, LibraryDescriptor>, Exception> {
		private ResolveVersionConflicts(Map<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> artifacts, LibraryDescriptorLoader resolver, WorkProgress progress, long work) {
			super("Resolve library version conflicts", Task.PRIORITY_IMPORTANT);
			this.artifacts = artifacts;
			this.resolver = resolver;
			this.progress = progress;
			this.work = work;
		}
		private Map<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> artifacts;
		private LibraryDescriptorLoader resolver;
		private WorkProgress progress;
		private long work;
		@Override
		public Map<String, LibraryDescriptor> run() throws Exception {
			if (progress != null) progress.setText("Resolving dependencies versions");
			app.getDefaultLogger().debug("Resolving version conflicts");
			Map<String, LibraryDescriptor> versions = new HashMap<>();
			for (Map.Entry<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> group : artifacts.entrySet()) {
				for (Map.Entry<String, List<TreeWithParent.Node<DependencyNode>>> artifact : group.getValue().entrySet()) {
					// create a mapping of versions, and remove errors
					Map<Version, List<TreeWithParent.Node<DependencyNode>>> artifactVersions = new HashMap<>();
					for (TreeWithParent.Node<DependencyNode> node : artifact.getValue()) {
						if (node.getElement().descr.hasError()) {
							app.getDefaultLogger().error("Dependency ignored: " + node.getElement().dep.getGroupId() + ':' + node.getElement().dep.getArtifactId() + " because of loading error", node.getElement().descr.getError());
							continue;
						}
						Version version = node.getElement().descr.getResult().getVersion();
						List<TreeWithParent.Node<DependencyNode>> nodes = artifactVersions.get(version);
						if (nodes == null) {
							nodes = new LinkedList<>();
							artifactVersions.put(version, nodes);
						}
						nodes.add(node);
					}
					// if only one remaining, no resolution needed
					if (artifactVersions.isEmpty()) {
						// error
						throw new Exception("Unable to load library " + group.getKey() + ':' + artifact.getKey());
					}
					if (artifactVersions.size() == 1) {
						Version version = artifactVersions.keySet().iterator().next();
						versions.put(group.getKey() + ':' + artifact.getKey(), artifactVersions.get(version).get(0).getElement().descr.getResult());
						continue;
					}
					Version version = resolver.resolveVersionConflict(group.getKey(), artifact.getKey(), artifactVersions);
					if (version == null) {
						// error
						throw new Exception("Unable to resolve version conflict for library " + group.getKey() + ':' + artifact.getKey());
					}
					if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug("Version conflict for " + group.getKey() + ':' + artifact.getKey() + " resolved to " + artifactVersions.get(version).get(0).getElement().descr.getResult().getVersionString());
					versions.put(group.getKey() + ':' + artifact.getKey(), artifactVersions.get(version).get(0).getElement().descr.getResult());
				}
			}
			// TODO progress by step
			if (progress != null) progress.progress(work);
			return versions;
		}
	}
	
	private class LoadLibrary extends Task.Cpu<Void, NoException> {
		private LoadLibrary(Lib lib, Map<String, LibraryDescriptor> versions, List<LibraryDescriptor> addPlugins) {
			super("Load library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), Task.PRIORITY_IMPORTANT);
			this.lib = lib;
			this.versions = versions;
			this.addPlugins = addPlugins;
		}
		private Lib lib;
		private Map<String, LibraryDescriptor> versions;
		private List<LibraryDescriptor> addPlugins;
		@Override
		public Void run() {
			if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug("Loading " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString());
			
			JoinPoint<Exception> jp = new JoinPoint<>();
			for (LibraryDescriptor.Dependency dep : lib.descr.getDependencies()) {
				String key = dep.getGroupId() + ':' + dep.getArtifactId();
				LibraryDescriptor d = versions.get(key);
				if (d == null) continue;
				Lib l;
				synchronized (libraries) {
					l = libraries.get(key);
					if (l != null) {
						jp.addToJoin(l.load);
						continue;
					}
					l = new Lib();
					l.descr = d;
					libraries.put(key, l);
				}
				new LoadLibrary(l, versions, null).start();
				jp.addToJoin(l.load);
			}
			if (addPlugins != null) {
				for (LibraryDescriptor d : addPlugins) {
					String key = d.getGroupId() + ':' + d.getArtifactId();
					Lib l;
					synchronized (libraries) {
						l = libraries.get(key);
						if (l != null) {
							jp.addToJoin(l.load);
							continue;
						}
						l = new Lib();
						l.descr = d;
						libraries.put(key, l);
					}
					new LoadLibrary(l, versions, null).start();
					jp.addToJoin(l.load);
				}
			}
			jp.start();
			File file = lib.descr.getClasses();
			if (file != null) {
				if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug(lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString() + " loaded from " + file.getAbsolutePath());
				lib.library = new LoadedLibrary(new Artifact(lib.descr.getGroupId(), lib.descr.getArtifactId(), lib.descr.getVersion()), appClassLoader.add(file, null));
				jp.listenAsync(new Init(lib), lib.load);
			} else {
				if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug("No classes in " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString());
				lib.library = new LoadedLibrary(new Artifact(lib.descr.getGroupId(), lib.descr.getArtifactId(), lib.descr.getVersion()), null);
				jp.listenInline(lib.load);
			}
			return null;
		}
	}
	
	private class Init extends Task.Cpu<Void, NoException> {
		private Init(Lib lib) {
			super("Initialize library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), Task.PRIORITY_IMPORTANT);
			this.lib = lib;
		}
		private Lib lib;
		@SuppressWarnings("resource")
		@Override
		public Void run() {
			if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug("Initializing " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString());
			
			JoinPoint<Exception> jp = new JoinPoint<>();
			// extension points
			AsyncWork<Void, Exception> ep = null;
			try {
				IO.Readable io = ((AbstractClassLoader)lib.library.getClassLoader()).provideReadableIO("META-INF/net.lecousin/extensionpoints", Task.PRIORITY_IMPORTANT);
				PreBufferedReadable bio = new PreBufferedReadable(io, 512, Task.PRIORITY_IMPORTANT, 1024, Task.PRIORITY_RATHER_IMPORTANT, 8);
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(bio, StandardCharsets.UTF_8, 256, 32);
				ep = new LoadLibraryExtensionPointsFile(stream, lib.library.getClassLoader()).start();
				jp.addToJoin(ep);
			} catch (FileNotFoundException e) {
			} catch (Throwable t) {
				lib.load.error(new Exception("Error reading file META-INF/net.lecousin/extensionpoints from library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
				return null;
			}
			// custom extension points
			ISynchronizationPoint<? extends Exception> previous = ep;
			for (CustomExtensionPoint custom : ExtensionPoints.getCustomExtensionPoints()) {
				String path = custom.getPluginConfigurationFilePath();
				if (path == null) continue;
				try {
					IO.Readable io = ((AbstractClassLoader)lib.library.getClassLoader()).provideReadableIO(path, Task.PRIORITY_IMPORTANT);
					previous = custom.loadPluginConfiguration(io, lib.library.getClassLoader(), previous);
					jp.addToJoin(previous);
				} catch (FileNotFoundException e) {
				} catch (Throwable t) {
					lib.load.error(new Exception("Error reading file " + path + " from library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
					return null;
				}
			}
			// plugins
			try {
				IO.Readable io = ((AbstractClassLoader)lib.library.getClassLoader()).provideReadableIO("META-INF/net.lecousin/plugins", Task.PRIORITY_IMPORTANT);
				PreBufferedReadable bio = new PreBufferedReadable(io, 512, Task.PRIORITY_IMPORTANT, 1024, Task.PRIORITY_RATHER_IMPORTANT, 8);
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(bio, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryPluginsFile task = new LoadLibraryPluginsFile(stream, lib.library.getClassLoader());
				SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
				if (previous == null)
					task.start().listenInlineSP(sp);
				else
					previous.listenInlineSP(() -> { task.start().listenInlineSP(sp); }, sp);
				jp.addToJoin(sp);
			} catch (FileNotFoundException e) {
			} catch (Throwable t) {
				lib.load.error(new Exception("Error reading file META-INF/net.lecousin/plugins from library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
				return null;
			}
			jp.start();
			jp.listenInline(new Runnable() {
				@Override
				public void run() {
					if (jp.hasError()) { if (!lib.load.hasError()) lib.load.error(jp.getError()); return; }
					lib.load.unblock();
				}
			});
			return null;
		}
	}

	@Override
	public ISynchronizationPoint<Exception> onLibrariesLoaded() {
		return canStartApp;
	}

	@Override
	public AsyncWork<LoadedLibrary, Exception> loadNewLibrary(String groupId, String artifactId, VersionSpecification version, boolean optional, byte priority, WorkProgress progress, long work) {
		// TODO progress
		// TODO lock to load only one library
		String key = groupId + ':' + artifactId;
		Lib l;
		synchronized (libraries) {
			Lib lib = libraries.get(key);
			if (lib != null) {
				if (lib.load.isUnblocked()) {
					if (lib.load.hasError())
						return new AsyncWork<>(null, lib.load.getError());
					return new AsyncWork<>(lib.library, null);
				}
				AsyncWork<LoadedLibrary, Exception> result = new AsyncWork<>();
				lib.load.listenInline(new Runnable() {
					@Override
					public void run() {
						if (lib.load.hasError())
							result.error(lib.load.getError());
						else
							result.unblockSuccess(lib.library);
					}
				});
				return result;
			}
			l = new Lib();
			libraries.put(key, l);
		}
		MutableInteger loaderIndex = new MutableInteger(0);
		AsyncWork<LoadedLibrary, Exception> result = new AsyncWork<>();
		Runnable nextLoader = new Runnable() {
			@Override
			public void run() {
				if (loaderIndex.get() == loaders.size()) {
					Exception error = new Exception("Cannot find library " + key);
					l.load.error(error);
					result.error(error);
					return;
				}
				AsyncWork<? extends LibraryDescriptor, Exception> loadDescr = loaders.get(loaderIndex.get()).loadLibrary(groupId, artifactId, version, priority);
				Runnable next = this;
				loadDescr.listenInline(new Runnable() {
					@Override
					public void run() {
						if (loadDescr.getResult() == null) {
							loaderIndex.inc();
							next.run();
							return;
						}
						l.descr = loadDescr.getResult();
						
						TreeWithParent<DependencyNode> tree = new TreeWithParent<>(null);
						Map<String, Map<String, List<TreeWithParent.Node<DependencyNode>>>> artifacts = new HashMap<>();
						JoinPoint<NoException> treeDone = new JoinPoint<>();
						// exclude libraries already loaded
						ArrayList<Pair<String, String>> exclusions = new ArrayList<>(libraries.size());
						for (Lib lib : libraries.values()) {
							if (lib.descr == null) continue;
							exclusions.add(new Pair<>(lib.descr.getGroupId(), lib.descr.getArtifactId()));
						}
						buildDependenciesTree(l.descr, tree, artifacts, exclusions, treeDone, null, null, 0);
						treeDone.start();
						ResolveVersionConflicts resolveConflicts = new ResolveVersionConflicts(artifacts, l.descr.getLoader(), null, 0);
						resolveConflicts.startOn(treeDone, true);
						resolveConflicts.getOutput().listenInline(new Runnable() {
							@Override
							public void run() {
								if (resolveConflicts.hasError()) { result.error(resolveConflicts.getError()); return; }

								app.getDefaultLogger().debug("Dependencies analyzed, loading and initializing libraries");

								LoadLibrary load = new LoadLibrary(l, resolveConflicts.getResult(), null);
								load.start();
								l.load.listenInline(new Runnable() {
									@Override
									public void run() {
										if (l.load.hasError())
											result.error(l.load.getError());
										else
											result.unblockSuccess(l.library);
									}
								});
							}
						});
					}
				});
			}
		};
		nextLoader.run();
		return result;
	}

	@Override
	public LoadedLibrary getLibrary(String groupId, String artifactId) {
		for (Lib lib : libraries.values())
			if (lib.library.getGroupId().equals(groupId) && lib.library.getArtifactId().equals(artifactId))
				return lib.library;
		return null;
	}

	@Override
	public IO.Readable getResource(String groupId, String artifactId, String path, byte priority) {
		if (groupId != null && artifactId != null) {
			LoadedLibrary lib = getLibrary(groupId, artifactId);
			if (lib == null)
				return null;
			return getResourceFrom(lib.getClassLoader(), path, priority);
		}
		return appClassLoader.getResourceIO(path, priority);
	}
	
	@Override
	public Readable getResource(String path, byte priority) {
		return appClassLoader.getResourceIO(path, priority);
	}

	public IO.Readable getResourceFrom(ClassLoader cl, String path, byte priority) {
		if (cl instanceof IOProviderFromName.Readable)
			try { return ((IOProviderFromName.Readable)cl).provideReadableIO(path, priority); }
			catch (Throwable t) {
				return null;
			}
		return appClassLoader.getResourceIO(path, priority);
	}

	@Override
	public List<File> getLibrariesLocations() {
		List<File> list = new ArrayList<>(libraries.size());
		for (Lib lib : libraries.values())
			getLibrariesLocations(list, lib);
		return list;
	}
	
	private void getLibrariesLocations(List<File> list, Lib lib) {
		File f = lib.descr.getClasses();
		if (f == null) return;
		if (list.contains(f)) return;
		for (Dependency dep : lib.descr.getDependencies()) {
			String key = dep.getGroupId() + ':' + dep.getArtifactId();
			Lib depLib = libraries.get(key);
			if (depLib == null) continue;
			getLibrariesLocations(list, depLib);
		}
		list.add(f);
	}
	
	
	/*
	private long start2;
	private MutableLong start3 = new MutableLong(0);
	void loadLibraries() {
		// add repository for plugins in development projects
		Repository.add(new PluginsRepositoryInMavenDevProjects());

		if (cfg.appClass == null) {
			canStartApp.error(new Exception("Missing class in application descriptor in "+cfg.dir.getAbsolutePath()));
			return;
		}

		WorkProgress prog = splash != null ? splash : new FakeWorkProgress();
		
		// free memory
		devPaths = null;
		
		start2 = System.nanoTime();
		app.getConsole().out("Application prepared in "+(start2-app.getStartTime())/1000000+"ms.");
		app.getConsole().out(Threading.printStats());
		libLoader = new LibraryLoader(defaultClassLoader, canStartApp);
		/*
		synchronized (libLoader) {
			libLoader.canStartLoading.unblock();
			libLoader.canStartLoading = null;
		}
		* /
		prog.setText("Loading application "+cfg.appName);
		AsyncWork<Library,Exception> load = libLoader.loadLibrary(appProject, prog, prog.getRemainingWork()*50/100);
		Task<Void,Exception> taskLoadPlugins = new Task.Cpu<Void,Exception>("Loading plugins", Task.PRIORITY_URGENT) {
			@Override
			public Void run() throws Exception {
				if (!load.isSuccessful()) {
					System.err.println("Error loading application: "+load.getError().getMessage());
					load.getError().printStackTrace(System.err);
					throw new Exception("Error loading application", load.getError());
				}
				
				prog.setText("Loading plugins");
				return null;
			}
		};
		load.listenAsynch(taskLoadPlugins, true);
		Task<Void,Exception> task = new Task.Cpu<Void,Exception>("Finishing to load libraries", Task.PRIORITY_URGENT) {
			@Override
			public Void run() {
				appLibrary = load.getResult();
				ExtensionPoints.allPluginsLoaded();
				start3.set(System.nanoTime());
				app.getConsole().out("Libraries loaded in "+(start3.get()-start2)/1000000+"ms.");
				app.getConsole().out(Threading.printStats());
				ExtensionPoints.logRemainingPlugins();
				return null;
			}
		};
		taskLoadPlugins.getSynch().listenAsynch(task, false);
		task.getSynch().listenInline(new Runnable() {
			@Override
			public void run() {
				if (task.isCancelled())
					canStartApp.cancel(task.getCancelEvent());
				else if (task.getSynch().hasError())
					canStartApp.error(task.getError());
				else
					canStartApp.start();
			}
		});
	}
	*/
	
	Task.Cpu<ISynchronizationPoint<Exception>, Exception> startApp() {
		Task.Cpu<ISynchronizationPoint<Exception>, Exception> task = new Task.Cpu<ISynchronizationPoint<Exception>, Exception>(app.getGroupId() + ':' + app.getArtifactId() + ':' + app.getVersion().toString(), Task.PRIORITY_NORMAL) {
			@Override
			public ISynchronizationPoint<Exception> run() throws Exception {
				if (splash != null) splash.setText("Starting application "+appCfg.name);
				@SuppressWarnings("rawtypes")
				Class cl;
				cl = appLib.library.getClassLoader().loadClass(appCfg.clazz);
				if (!ApplicationBootstrap.class.isAssignableFrom(cl))
					throw new Exception("Application class "+appCfg.clazz+" must implements ApplicationBootstrap");
				ApplicationBootstrap startup = (ApplicationBootstrap)cl.newInstance();
				WorkProgress progress = splash != null ? splash : new FakeWorkProgress();
				ISynchronizationPoint<Exception> start = startup.start(app, progress);
				
				progress.getSynch().listenInline(new Runnable() {
					@Override
					public void run() {
						/*
						long end = System.nanoTime();
						app.getConsole().out("Application started in "+(end-app.getStartTime())/1000000+"ms. ("+(start2-app.getStartTime())/1000000+"ms. of preparation, "+(start3.get()-start2)/1000000+"ms. loading libraries and "+(end-start3.get())/1000000+"ms. initializing application)");
						app.getConsole().out(Threading.printStats());
						*/
						splash = null;
					}
				});
				return start;
			}
		};
		task.start();
		return task;
	}
	
}
