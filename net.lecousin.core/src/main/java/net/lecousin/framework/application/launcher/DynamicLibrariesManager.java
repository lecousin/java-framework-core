package net.lecousin.framework.application.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.ImageIcon;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationBootstrap;
import net.lecousin.framework.application.ApplicationBootstrapException;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.SplashScreen;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.VersionSpecification.SingleVersion;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.ArtifactsLibrariesManager;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor.Dependency;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader.DependencyNode;
import net.lecousin.framework.application.libraries.artifacts.LoadedLibrary;
import net.lecousin.framework.application.libraries.classloader.AbstractClassLoader;
import net.lecousin.framework.application.libraries.classloader.AppClassLoader;
import net.lecousin.framework.application.libraries.classpath.LoadLibraryExtensionPointsFile;
import net.lecousin.framework.application.libraries.classpath.LoadLibraryPluginsFile;
import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.collections.Tree;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.tasks.drives.FullReadFileTask;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromPathUsingClassloader;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.progress.FakeWorkProgress;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;

/**
 * Libraries manager for development launcher.
 */
public class DynamicLibrariesManager implements ArtifactsLibrariesManager {

	/** Constructor. */
	public DynamicLibrariesManager(
		List<File> devPaths, SplashScreen splash, List<LibraryDescriptorLoader> loaders,
		File appDir, ApplicationConfiguration cfg, List<Triple<String, String, String>> plugins
	) {
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
	private List<File> devPaths;
	private SplashScreen splash;
	private List<LibraryDescriptorLoader> loaders;
	private JoinPoint<LibraryManagementException> canStartApp = new JoinPoint<>();
	private Lib appLib;
	private ApplicationConfiguration appCfg;
	private List<Triple<String, String, String>> loadPlugins;
	
	private static class Lib {
		private LibraryDescriptor descr;
		private Async<LibraryManagementException> load = new Async<>();
		private LoadedLibrary library;
	}
	
	private Map<String, Lib> libraries = new HashMap<>();
	
	@Override
	public ApplicationClassLoader start(Application app) {
		this.app = app;
		this.appClassLoader = new AppClassLoader(app);
		
		app.getDefaultLogger().debug("Start loading application in development mode");
		
		long work = splash != null ? splash.getRemainingWork() : 0;
		work = work - work * 40 / 100; // loading is 60%, 40% will be application startup
		long stepDevProjects = devPaths != null ? work / 20 : 0; // 5%
		long stepDependencies = work * (devPaths != null ? 80 : 85) / 100; // 80% for dev, else 85%
		long stepVersionConflicts = work - stepDevProjects - stepDependencies; // 15%
		// TODO LoadLibrary should make progress
		
		if (splash != null) {
			if (appCfg.getSplash() != null) {
				File splashFile = new File(appDir, appCfg.getSplash());
				if (splashFile.exists())
					loadSplashFile(splashFile);
				else
					splash.loadDefaultLogo();
			} else {
				splash.loadDefaultLogo();
			}
		}
		
		if (devPaths != null)
			developmentMode(stepDevProjects, stepDependencies, stepVersionConflicts);
		else
			productionMode(stepDependencies, stepVersionConflicts);
		return this.appClassLoader;
	}
	
	private void loadSplashFile(File splashFile) {
		FullReadFileTask read = new FullReadFileTask(splashFile, Task.PRIORITY_URGENT);
		read.start();
		Task<Void,NoException> load = new Task.Cpu<Void,NoException>("Loading splash image", Task.PRIORITY_URGENT) {
			@Override
			public Void run() {
				ImageIcon img = new ImageIcon(read.getResult());
				if (splash == null) return null;
				synchronized (splash) {
					while (!splash.isReady())
						try { splash.wait(); }
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return null;
						}
				}
				splash.setLogo(img, true);
				return null;
			}
		};
		read.ondone(load, false);
	}
	
	private void developmentMode(long stepDevProjects, long stepDependencies, long stepVersionConflicts) {
		// load dev projects
		if (splash != null)
			splash.setText("Analyzing development projects");
		AsyncSupplier<List<LibraryDescriptor>, LibraryManagementException> devProjects = loadDevProjects(stepDevProjects);
		// get application project
		devProjects.thenStart("Load application libraries", Task.PRIORITY_IMPORTANT,
			() -> loadDevApp(devProjects, stepDependencies, stepVersionConflicts), canStartApp);
	}
	
	private AsyncSupplier<List<LibraryDescriptor>, LibraryManagementException> loadDevProjects(long stepDevProjects) {
		JoinPoint<LibraryManagementException> jpDevProjects = new JoinPoint<>();
		ArrayList<AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException>> devProjects = new ArrayList<>(devPaths.size());
		new Task.Cpu.FromRunnable("Load development projects", Task.PRIORITY_IMPORTANT, () -> {
			int nb = devPaths.size();
			long w = stepDevProjects;
			for (File dir : devPaths) {
				long step = w / nb--;
				w -= step;
				AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException> load =
					CollectionsUtil.<LibraryDescriptorLoader>filterSingle(loader -> loader.detect(dir))
					.andThen(loader -> loader != null ? loader.loadProject(dir, Task.PRIORITY_IMPORTANT) : null)
					.apply(loaders);
				if (load == null) {
					app.getDefaultLogger().error("Unknown type of project: " + dir.getAbsolutePath());
					if (splash != null) splash.progress(step);
					continue;
				}
				if (splash != null)
					load.onDone(() -> splash.progress(step));
				devProjects.add(load);
				jpDevProjects.addToJoin(load);
			}
			jpDevProjects.start();
			// free memory
			devPaths = null;
		}).start();
		AsyncSupplier<List<LibraryDescriptor>, LibraryManagementException> result = new AsyncSupplier<>();
		jpDevProjects.onDone(() -> result.unblockSuccess(CollectionsUtil.map(devProjects, AsyncSupplier::getResult)), result);
		return result;
	}
	
	private void loadDevApp(
		AsyncSupplier<List<LibraryDescriptor>, LibraryManagementException> devProjects,
		long stepDependencies, long stepVersionConflicts
	) {
		LibraryDescriptor appLibDescr = null;
		for (LibraryDescriptor lib : devProjects.getResult()) {
			if (lib != null &&
				app.getGroupId().equals(lib.getGroupId()) &&
				app.getArtifactId().equals(lib.getArtifactId())) {
				appLibDescr = lib;
				break;
			}
		}
		if (appLibDescr == null) {
			canStartApp.error(new LibraryManagementException(
				"Cannot find application " + app.getGroupId() + ':' + app.getArtifactId()));
			return;
		}
		if (!appLibDescr.hasClasses()) {
			canStartApp.error(new LibraryManagementException("Application project must provide classes"));
			return;
		}
		
		app.getDefaultLogger().debug("Development projects analyzed, loading application");
		
		List<LibraryDescriptor> addPlugins = new LinkedList<>();
		for (Triple<String, String, String> t : loadPlugins) {
			app.getDefaultLogger().debug("Searching projects matching " + t.getValue1() + ':' + t.getValue2() + ':' + t.getValue3());
			for (LibraryDescriptor lib : devProjects.getResult()) {
				if (lib.getGroupId().equals(t.getValue1()) &&
					(t.getValue2() == null || lib.getArtifactId().equals(t.getValue2())) &&
					(t.getValue3() == null || lib.getVersionString().equals(t.getValue3()))) {
					addPlugins.add(lib);
					app.getDefaultLogger().debug("Plug-in to load: " + lib.toString());
				}
			}
		}
		
		// load library
		loadApplicationLibrary(appLibDescr, addPlugins, stepDependencies, stepVersionConflicts);
	}
	
	private void productionMode(long stepDependencies, long stepVersionConflicts) {
		searchApplication(0, stepDependencies, stepVersionConflicts);
	}
	
	private void searchApplication(int loaderIndex, long stepDependencies, long stepVersionConflicts) {
		if (loaderIndex == loaders.size()) {
			canStartApp.error(new LibraryManagementException("Application not found"));
			return;
		}
		AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException> load = loaders.get(loaderIndex).loadLibrary(
			app.getGroupId(), app.getArtifactId(), new SingleVersion(app.getVersion()),
			Task.PRIORITY_IMPORTANT, new ArrayList<>(0));
		load.onDone(() -> {
			if (!load.isSuccessful() || load.getResult() == null) {
				searchApplication(loaderIndex + 1, stepDependencies, stepVersionConflicts);
				return;
			}
			LibraryDescriptor appLibDescr = load.getResult();
			if (!appLibDescr.hasClasses()) {
				canStartApp.error(new LibraryManagementException("Application project must provide classes"));
				return;
			}
			app.getDefaultLogger().debug("Loading application");
			
			List<LibraryDescriptor> addPlugins = new LinkedList<>();
			/* TODO
			for (Triple<String, String, String> t : loadPlugins) {
				for (AsyncWork<? extends LibraryDescriptor, Exception> p : devProjects) {
					LibraryDescriptor lib = p.getResult();
					if (!lib.getGroupId().equals(t.getValue1())) continue;
					if (t.getValue2() != null && !lib.getArtifactId().equals(t.getValue2())) continue;
					if (t.getValue3() != null && !lib.getVersionString().equals(t.getValue3())) continue;
					addPlugins.add(lib);
				}
			}*/
			
			// load library
			loadApplicationLibrary(appLibDescr, addPlugins, stepDependencies, stepVersionConflicts);
		});
	}
	
	private void loadApplicationLibrary(
		LibraryDescriptor descr, List<LibraryDescriptor> addPlugins, long stepDependencies, long stepVersionConflicts
	) {
		app.getDefaultLogger().debug("Building dependencies tree");
		if (splash != null) splash.setText("Analyzing dependencies");

		Tree.WithParent<DependencyNode> tree = new Tree.WithParent<>(null);
		Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts = new HashMap<>();
		JoinPoint<NoException> treeDone = new JoinPoint<>();
		buildDependenciesTree(descr, tree, artifacts, new ArrayList<>(0), treeDone, addPlugins, splash, stepDependencies);
		treeDone.start();
		ResolveVersionConflicts resolveConflicts = new ResolveVersionConflicts(artifacts, descr.getLoader(), splash, stepVersionConflicts);
		resolveConflicts.startOn(treeDone, true);
		resolveConflicts.getOutput().onDone(() -> {
			app.getDefaultLogger().debug("Dependencies analyzed, loading and initializing libraries");
			
			if (splash != null) splash.setText("Initializing libraries");

			Lib lib = new Lib();
			lib.descr = descr;
			libraries.put(descr.getGroupId() + ':' + descr.getArtifactId(), lib);
			appLib = lib;
			new LoadLibrary(lib, resolveConflicts.getResult(), addPlugins).start();
			lib.load.thenStart(new Task.Cpu<Void, NoException>("Finishing to initialize", Task.PRIORITY_IMPORTANT) {
				@Override
				public Void run() {
					if (canStartApp.hasError()) return null;
					app.getDefaultLogger().debug("Libraries initialized.");
					ExtensionPoints.allPluginsLoaded();
					canStartApp.unblock();
					return null;
				}
			}, canStartApp);
		}, canStartApp);
	}

	@SuppressWarnings({"squid:S2445", "squid:S00107"})
	private void buildDependenciesTree(
		LibraryDescriptor descr, Tree.WithParent<DependencyNode> tree,
		Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts,
		Collection<Pair<String, String>> exclusions, JoinPoint<NoException> jp,
		List<LibraryDescriptor> addPlugins,
		WorkProgress progress, long work) {
		// TODO keep optional that were not found to avoid searching again and again
		List<LibraryDescriptor.Dependency> deps = getDependenciesWithPlugins(descr, addPlugins);
		int nb = deps.size();
		for (LibraryDescriptor.Dependency dep : deps) {
			if (isMatching(dep.getGroupId(), dep.getArtifactId(), exclusions)) {
				nb--;
			} else {
				long step = work / nb--;
				work -= step;
				app.getDefaultLogger().debug(
					"Dependency: " + descr.getGroupId() + ':' + descr.getArtifactId() + ':'
					+ descr.getVersionString() + " => " + dep.getGroupId() + ':' + dep.getArtifactId()
					+ ':' + dep.getVersionSpecification());
				buildDependenciesTreeForDependency(descr, tree, artifacts, exclusions, jp, dep, progress, step);
			}
		}
		if (progress != null && work > 0) progress.progress(work);
	}
	
	private static List<LibraryDescriptor.Dependency> getDependenciesWithPlugins(LibraryDescriptor descr, List<LibraryDescriptor> addPlugins) {
		List<LibraryDescriptor.Dependency> deps = descr.getDependencies();
		if (addPlugins == null)
			return deps;
		ArrayList<LibraryDescriptor.Dependency> newDeps = new ArrayList<>(deps.size() + addPlugins.size());
		newDeps.addAll(deps);
		for (LibraryDescriptor l : addPlugins)
			newDeps.add(new LibraryDescriptor.Dependency.From(l));
		return newDeps;
	}
	
	@SuppressWarnings("squid:S00107") // number of arguments
	private void buildDependenciesTreeForDependency(
		LibraryDescriptor descr, Tree.WithParent<DependencyNode> tree,
		Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts,
		Collection<Pair<String, String>> exclusions, JoinPoint<NoException> jp,
		LibraryDescriptor.Dependency dep,
		WorkProgress progress, long step
	) {
		DependencyNode node = createDependencyNode(descr, dep);
		Tree.Node<DependencyNode> n;
		synchronized (tree) { n = tree.add(node); }
		List<Tree.Node<DependencyNode>> l = getDependenciesListFor(dep, n, artifacts);
		jp.addToJoin(1);
		node.getDescriptor().onDone(() -> {
			if (node.getDescriptor().getResult() != null) {
				Set<Pair<String, String>> excl = new HashSet<>();
				excl.addAll(exclusions);
				excl.addAll(dep.getExcludedDependencies());
				if (progress != null) progress.progress(step / 2);
				buildDependenciesTree(node.getDescriptor().getResult(), (Tree.WithParent<DependencyNode>)n.getSubNodes(),
					artifacts, excl, jp, null, progress, step - step / 2);
			} else {
				if (progress != null) progress.progress(step);
				if (dep.isOptional()) {
					// optional => no error
					app.getDefaultLogger().debug(
						"Dependency " + dep.getGroupId() + ':' + dep.getArtifactId() + ':'
						+ dep.getVersionSpecification() + " not found, but optional");
					synchronized (tree) {
						tree.removeInstance(node);
					}
					removeArtifact(l, n, dep, artifacts);
				}
			}
			jp.joined();
		});
	}
	
	private static boolean isMatching(String groupId, String artifactId, Collection<Pair<String, String>> list) {
		for (Pair<String, String> p : list) {
			if ((p.getValue1() == null || p.getValue1().equals(groupId)) &&
				(p.getValue2() == null || p.getValue2().equals(artifactId)))
				return true;
		}
		return false;
	}
	
	private static DependencyNode createDependencyNode(LibraryDescriptor descr, LibraryDescriptor.Dependency dep) {
		DependencyNode node = new DependencyNode(dep);
		if (dep.getGroupId() == null || dep.getGroupId().length() == 0)
			node.setDescriptor(new AsyncSupplier<>(null, new LibraryManagementException("Missing groupId in dependency")));
		else if (dep.getArtifactId() == null || dep.getArtifactId().length() == 0)
			node.setDescriptor(new AsyncSupplier<>(null, new LibraryManagementException("Missing artifactId in dependency")));
		else {
			VersionSpecification depV = dep.getVersionSpecification();
			if (depV == null)
				node.setDescriptor(new AsyncSupplier<>(null, new LibraryManagementException("Missing version in dependency")));
			else
				node.setDescriptor(descr.getLoader().loadLibrary(
					dep.getGroupId(), dep.getArtifactId(), depV,
					Task.PRIORITY_RATHER_IMPORTANT, descr.getDependenciesAdditionalRepositories()));
		}
		return node;
	}
	
	private static List<Tree.Node<DependencyNode>> getDependenciesListFor(
		LibraryDescriptor.Dependency dep, Tree.Node<DependencyNode> n,
		Map<String,Map<String, List<Tree.Node<DependencyNode>>>> artifacts
	) {
		synchronized (artifacts) {
			Map<String, List<Tree.Node<DependencyNode>>> group = artifacts.get(dep.getGroupId());
			if (group == null) {
				group = new HashMap<>();
				artifacts.put(dep.getGroupId(), group);
			}
			List<Tree.Node<DependencyNode>> list = group.get(dep.getArtifactId());
			if (list == null) {
				list = new LinkedList<>();
				group.put(dep.getArtifactId(), list);
			}
			list.add(n);
			return list;
		}
	}
	
	private static void removeArtifact(
		List<Tree.Node<DependencyNode>> list,
		Tree.Node<DependencyNode> n,
		LibraryDescriptor.Dependency dep,
		Map<String,Map<String, List<Tree.Node<DependencyNode>>>> artifacts
	) {
		synchronized (artifacts) {
			list.remove(n);
			if (list.isEmpty()) {
				Map<String, List<Tree.Node<DependencyNode>>> group =
					artifacts.get(dep.getGroupId());
				group.remove(dep.getArtifactId());
				if (group.isEmpty())
					artifacts.remove(dep.getGroupId());
			}
		}
	}
	
	private class ResolveVersionConflicts extends Task.Cpu<Map<String, LibraryDescriptor>, LibraryManagementException> {
		private ResolveVersionConflicts(
			Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts,
			LibraryDescriptorLoader resolver, WorkProgress progress, long work
		) {
			super("Resolve library version conflicts", Task.PRIORITY_IMPORTANT);
			this.artifacts = artifacts;
			this.resolver = resolver;
			this.progress = progress;
			this.work = work;
		}
		
		private Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts;
		private LibraryDescriptorLoader resolver;
		private WorkProgress progress;
		private long work;
		
		@Override
		public Map<String, LibraryDescriptor> run() throws LibraryManagementException {
			if (progress != null) progress.setText("Resolving dependencies versions");
			app.getDefaultLogger().debug("Resolving version conflicts");
			Map<String, LibraryDescriptor> versions = new HashMap<>();
			for (Map.Entry<String, Map<String, List<Tree.Node<DependencyNode>>>> group : artifacts.entrySet()) {
				for (Map.Entry<String, List<Tree.Node<DependencyNode>>> artifact : group.getValue().entrySet()) {
					// create a mapping of versions, and remove errors
					Map<Version, List<Tree.Node<DependencyNode>>> artifactVersions = getArtifactVersions(artifact);
					if (artifactVersions.isEmpty()) {
						// error
						throw new LibraryManagementException("Unable to load library "
							+ group.getKey() + ':' + artifact.getKey());
					}
					// if only one remaining, no resolution needed
					if (artifactVersions.size() == 1) {
						Version version = artifactVersions.keySet().iterator().next();
						versions.put(group.getKey() + ':' + artifact.getKey(),
							artifactVersions.get(version).get(0).getElement().getDescriptor().getResult());
						continue;
					}
					Version version = resolver.resolveVersionConflict(group.getKey(), artifact.getKey(), artifactVersions);
					if (version == null) {
						// error
						throw new LibraryManagementException("Unable to resolve version conflict for library "
							+ group.getKey() + ':' + artifact.getKey());
					}
					if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug(
						"Version conflict for " + group.getKey() + ':' + artifact.getKey() + " resolved to "
						+ artifactVersions.get(version).get(0).getElement().getDescriptor().getResult().getVersionString());
					versions.put(group.getKey() + ':' + artifact.getKey(),
						artifactVersions.get(version).get(0).getElement().getDescriptor().getResult());
				}
			}
			// TODO progress by step
			if (progress != null) progress.progress(work);
			return versions;
		}
		
		private Map<Version, List<Tree.Node<DependencyNode>>> getArtifactVersions(
			Map.Entry<String, List<Tree.Node<DependencyNode>>> artifact) {
			Map<Version, List<Tree.Node<DependencyNode>>> artifactVersions = new HashMap<>();
			for (Tree.Node<DependencyNode> node : artifact.getValue()) {
				if (node.getElement().getDescriptor().hasError()) {
					app.getDefaultLogger().error(
						"Dependency ignored: " + node.getElement().getDependency().getGroupId() + ':'
						+ node.getElement().getDependency().getArtifactId() + " because of loading error",
						node.getElement().getDescriptor().getError());
					continue;
				}
				Version version = node.getElement().getDescriptor().getResult().getVersion();
				List<Tree.Node<DependencyNode>> nodes = artifactVersions.get(version);
				if (nodes == null) {
					nodes = new LinkedList<>();
					artifactVersions.put(version, nodes);
				}
				nodes.add(node);
			}
			return artifactVersions;
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
			if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug(
				"Loading " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString());
			
			JoinPoint<LibraryManagementException> jp = new JoinPoint<>();
			for (LibraryDescriptor.Dependency dep : lib.descr.getDependencies()) {
				String key = dep.getGroupId() + ':' + dep.getArtifactId();
				LibraryDescriptor d = versions.get(key);
				if (d == null) continue;
				load(d, key, jp);
			}
			if (addPlugins != null) {
				for (LibraryDescriptor d : addPlugins) {
					String key = d.getGroupId() + ':' + d.getArtifactId();
					load(d, key, jp);
				}
			}
			jp.start();
			
			lib.descr.getClasses().onDone(file -> {
				if (file != null) {
					if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug(
						lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString()
						+ " loaded from " + file.getAbsolutePath());
					lib.library = new LoadedLibrary(new Artifact(lib.descr.getGroupId(), lib.descr.getArtifactId(),
						lib.descr.getVersion()), appClassLoader.add(file, null));
					jp.thenStart(new Init(lib), lib.load);
				} else {
					if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug("No classes in " + lib.descr.getGroupId()
						+ ':' + lib.descr.getArtifactId() + ':' + lib.descr.getVersionString());
					lib.library = new LoadedLibrary(new Artifact(lib.descr.getGroupId(), lib.descr.getArtifactId(),
						lib.descr.getVersion()), null);
					jp.onDone(lib.load);
				}
			});
			return null;
		}
		
		private void load(LibraryDescriptor d, String key, JoinPoint<LibraryManagementException> jp) {
			Lib l;
			synchronized (libraries) {
				l = libraries.get(key);
				if (l != null) {
					jp.addToJoin(l.load);
					return;
				}
				l = new Lib();
				l.descr = d;
				libraries.put(key, l);
			}
			new LoadLibrary(l, versions, null).start();
			jp.addToJoin(l.load);
		}
	}
	
	private class Init extends Task.Cpu<Void, NoException> {
		private Init(Lib lib) {
			super("Initialize library " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), Task.PRIORITY_IMPORTANT);
			this.lib = lib;
		}
		
		private Lib lib;
		private IAsync<Exception> previousStep = null;
		
		@Override
		public Void run() {
			if (app.getDefaultLogger().debug()) app.getDefaultLogger().debug(
				"Initializing " + lib.descr.getGroupId() + ':' + lib.descr.getArtifactId() + ':'
				+ lib.descr.getVersionString());
			
			JoinPoint<LibraryManagementException> jp = new JoinPoint<>();
			
			// extension points
			if (!loadExtensionPoints(jp))
				return null;
			
			// custom extension points
			if (!loadCustomExtensionPoints(jp))
				return null;
			
			// plugins
			if (!loadPlugins(jp))
				return null;
			
			jp.start();
			jp.onDone(() -> {
				if (jp.hasError()) {
					if (!lib.load.hasError()) lib.load.error(jp.getError());
					return;
				}
				lib.load.unblock();
			});
			return null;
		}
		
		private boolean loadExtensionPoints(JoinPoint<LibraryManagementException> jp) {
			AsyncSupplier<Void, Exception> ep = null;
			IO.Readable io;
			try {
				io = ((AbstractClassLoader)lib.library.getClassLoader())
					.open("META-INF/net.lecousin/extensionpoints", Task.PRIORITY_IMPORTANT);
			} catch (FileNotFoundException e) {
				// ignore
				io = null;
			} catch (Exception t) {
				lib.load.error(new LibraryManagementException("Error reading file META-INF/net.lecousin/extensionpoints from library "
					+ lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
				return false;
			}
			if (io != null) {
				PreBufferedReadable bio = new PreBufferedReadable(io, 512, Task.PRIORITY_IMPORTANT, 1024,
					Task.PRIORITY_RATHER_IMPORTANT, 8);
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(bio, StandardCharsets.UTF_8, 256, 32);
				ep = new LoadLibraryExtensionPointsFile(stream, lib.library.getClassLoader()).start();
				jp.addToJoin(ep, error -> new LibraryManagementException("Error loading extension points from " + lib, error));
				stream.closeAfter(ep);
				previousStep = ep;
			}
			return true;
		}

		private boolean loadCustomExtensionPoints(JoinPoint<LibraryManagementException> jp) {
			for (CustomExtensionPoint custom : ExtensionPoints.getCustomExtensionPoints()) {
				String path = custom.getPluginConfigurationFilePath();
				if (path == null) continue;
				IO.Readable io = null;
				try {
					io = ((AbstractClassLoader)lib.library.getClassLoader()).open(path, Task.PRIORITY_IMPORTANT);
				} catch (FileNotFoundException e) {
					// ignore
				} catch (Exception t) {
					lib.load.error(new LibraryManagementException("Error reading file " + path + " from library "
						+ lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
					return false;
				}
				if (io != null) {
					previousStep = custom.loadPluginConfiguration(io, lib.library.getClassLoader(), previousStep);
					jp.addToJoin(previousStep,
						error -> new LibraryManagementException("Error loading plugin from " + lib, error));
					io.closeAfter(previousStep);
				}
			}
			return true;
		}
		
		private boolean loadPlugins(JoinPoint<LibraryManagementException> jp) {
			IO.Readable io = null;
			try {
				io = ((AbstractClassLoader)lib.library.getClassLoader())
					.open("META-INF/net.lecousin/plugins", Task.PRIORITY_IMPORTANT);
			} catch (FileNotFoundException e) {
				// ignore
			} catch (Exception t) {
				lib.load.error(new LibraryManagementException("Error reading file META-INF/net.lecousin/plugins from library "
					+ lib.descr.getGroupId() + ':' + lib.descr.getArtifactId(), t));
				return false;
			}
			if (io != null) {
				PreBufferedReadable bio = new PreBufferedReadable(io, 512, Task.PRIORITY_IMPORTANT, 1024,
					Task.PRIORITY_RATHER_IMPORTANT, 8);
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(bio, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryPluginsFile task = new LoadLibraryPluginsFile(stream, lib.library.getClassLoader());
				Async<Exception> sp = new Async<>();
				if (previousStep == null)
					task.start().onDone(sp);
				else
					previousStep.onDone(() -> task.start().onDone(sp), sp);
				jp.addToJoin(sp, error -> new LibraryManagementException("Error loading plugin from " + lib, error));
				io.closeAfter(sp);
			}
			return true;
		}
	}

	@Override
	public IAsync<LibraryManagementException> onLibrariesLoaded() {
		return canStartApp;
	}

	@Override
	public AsyncSupplier<LoadedLibrary, LibraryManagementException> loadNewLibrary(
		String groupId, String artifactId, VersionSpecification version, boolean optional, byte priority,
		WorkProgress progress, long work
	) {
		// TODO progress
		// TODO lock to load only one library
		String key = groupId + ':' + artifactId;
		Lib l;
		synchronized (libraries) {
			Lib lib = libraries.get(key);
			if (lib != null) {
				if (lib.load.isDone()) {
					if (lib.load.hasError())
						return new AsyncSupplier<>(null, lib.load.getError());
					return new AsyncSupplier<>(lib.library, null);
				}
				AsyncSupplier<LoadedLibrary, LibraryManagementException> result = new AsyncSupplier<>();
				lib.load.onDone(() -> {
					if (lib.load.hasError())
						result.error(lib.load.getError());
					else
						result.unblockSuccess(lib.library);
				});
				return result;
			}
			l = new Lib();
			libraries.put(key, l);
		}
		final MutableInteger loaderIndex = new MutableInteger(0);
		AsyncSupplier<LoadedLibrary, LibraryManagementException> result = new AsyncSupplier<>();
		Runnable nextLoader = new Runnable() {
			@Override
			public void run() {
				if (loaderIndex.get() == loaders.size()) {
					LibraryManagementException error = new LibraryManagementException("Cannot find library " + key);
					l.load.error(error);
					result.error(error);
					return;
				}
				AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException> loadDescr =
					loaders.get(loaderIndex.get()).loadLibrary(groupId, artifactId, version, priority, new ArrayList<>(0));
				Runnable next = this;
				loadDescr.onDone(() -> {
					if (loadDescr.getResult() == null) {
						loaderIndex.inc();
						next.run();
						return;
					}
					l.descr = loadDescr.getResult();
					
					Tree.WithParent<DependencyNode> tree = new Tree.WithParent<>(null);
					Map<String, Map<String, List<Tree.Node<DependencyNode>>>> artifacts = new HashMap<>();
					JoinPoint<NoException> treeDone = new JoinPoint<>();
					// exclude libraries already loaded
					ArrayList<Pair<String, String>> exclusions = new ArrayList<>(libraries.size());
					for (Lib lib : libraries.values()) {
						if (lib.descr == null) continue;
						exclusions.add(new Pair<>(lib.descr.getGroupId(), lib.descr.getArtifactId()));
					}
					buildDependenciesTree(l.descr, tree, artifacts, exclusions, treeDone, null, null, 0);
					treeDone.start();
					ResolveVersionConflicts resolveConflicts =
						new ResolveVersionConflicts(artifacts, l.descr.getLoader(), null, 0);
					resolveConflicts.startOn(treeDone, true);
					resolveConflicts.getOutput().onDone(() -> {
						app.getDefaultLogger().debug("Dependencies analyzed, loading and initializing libraries");

						LoadLibrary load = new LoadLibrary(l, resolveConflicts.getResult(), null);
						load.start();
						l.load.onDone(result, () -> l.library);
					}, result);
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

	/** Open a resource from the given class loader. */
	public IO.Readable getResourceFrom(ClassLoader cl, String path, byte priority) {
		IOProvider.Readable provider = new IOProviderFromPathUsingClassloader(cl).get(path);
		if (provider == null)
			return null;
		try {
			return provider.provideIOReadable(priority);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public List<File> getLibrariesLocations() {
		List<File> list = new ArrayList<>(libraries.size());
		for (Lib lib : libraries.values())
			getLibrariesLocations(list, lib);
		return list;
	}
	
	private void getLibrariesLocations(List<File> list, Lib lib) {
		File f;
		try { f = lib.descr.getClasses().blockResult(0); }
		catch (Exception e) {
			return;
		}
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
	
	Task.Cpu<IAsync<Exception>, ApplicationBootstrapException> startApp() {
		Task.Cpu<IAsync<Exception>, ApplicationBootstrapException> task =
			new Task.Cpu<IAsync<Exception>, ApplicationBootstrapException>(
				app.getGroupId() + ':' + app.getArtifactId() + ':' + app.getVersion().toString(), Task.PRIORITY_NORMAL
			) {
			@Override
			public IAsync<Exception> run() throws ApplicationBootstrapException {
				if (splash != null) splash.setText("Starting application " + appCfg.getName());
				@SuppressWarnings("rawtypes")
				Class cl;
				try {
					cl = appLib.library.getClassLoader().loadClass(appCfg.getClazz());
				} catch (ClassNotFoundException e) {
					throw new ApplicationBootstrapException("Application class does not exist", e);
				}
				if (!ApplicationBootstrap.class.isAssignableFrom(cl))
					throw new ApplicationBootstrapException(
						"Application class " + appCfg.getClazz() + " must implements ApplicationBootstrap");
				ApplicationBootstrap startup;
				try {
					startup = (ApplicationBootstrap)cl.newInstance();
				} catch (Exception e) {
					throw new ApplicationBootstrapException("Application class cannot be instantiated", e);
				}
				WorkProgress progress = splash != null ? splash : new FakeWorkProgress();
				IAsync<Exception> start = startup.start(app, progress);
				
				progress.getSynch().onDone(() ->
					/*
					long end = System.nanoTime();
					app.getConsole().out("Application started in "+(end-app.getStartTime())/1000000+"ms. ("
					+(start2-app.getStartTime())/1000000+"ms. of preparation, "+(start3.get()-start2)/1000000
					+"ms. loading libraries and "+(end-start3.get())/1000000+"ms. initializing application)");
					app.getConsole().out(Threading.printStats());
					*/
					splash = null
				);
				return start;
			}
		};
		task.start();
		return task;
	}
	
	@Override
	public void scanLibraries(
		String rootPackage, boolean includeSubPackages, Predicate<String> packageFilter, Predicate<String> classFilter,
		Consumer<Class<?>> classScanner
	) {
		appClassLoader.scanLibraries(rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
	}
	
}
