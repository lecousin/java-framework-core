package net.lecousin.framework.application.launcher;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.SplashScreen;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.application.libraries.artifacts.maven.MavenLocalRepository;
import net.lecousin.framework.application.libraries.artifacts.maven.MavenPOMLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.util.Triple;

/**
 * Application launcher.
 */
public class AppLauncher {

	/** Execution entry point. */
	public static void main(String[] args) {
		// start the splash screen as soon as possible
		SplashScreen splashScreen = null;
		if (!GraphicsEnvironment.isHeadless() && !"true".equals(System.getProperty("nosplash")))
			splashScreen = new SplashScreen(true);

		final SplashScreen splash = splashScreen;
		//-XstartOnFirstThread
		LCCore.keepMainThread(new Runnable() {
			@Override
			public void run() {
				MavenPOMLoader pomLoader = new MavenPOMLoader();
				ArrayList<LibraryDescriptorLoader> loaders = new ArrayList<>(1);
				loaders.add(pomLoader);
				List<LibrariesRepository> repositories = new LinkedList<>();
				
				String groupId = null;
				String artifactId = null;
				String version = null;
				String plugins = null;
				String[] appParameters = new String[0];
				for (int i = 0; i < args.length; ++i) {
					if (args[i].startsWith("-groupId="))
						groupId = args[i].substring(9);
					else if (args[i].startsWith("-artifactId="))
						artifactId = args[i].substring(12);
					else if (args[i].startsWith("-version="))
						version = args[i].substring(9);
					else if (args[i].startsWith("-plugins="))
						plugins = args[i].substring(9);
					else if (args[i].startsWith("-maven-repository=")) {
						String path = args[i].substring(18);
						File dir = new File(path);
						if (dir.exists()) {
							MavenLocalRepository repo = new MavenLocalRepository(dir, true, true);
							repositories.add(repo);
							pomLoader.addRepository(repo);
						}
					} else if (args[i].equals("-parameters")) {
						appParameters = new String[args.length - i - 1];
						for (int j = 0; j < appParameters.length; ++j)
							appParameters[j] = args[i + 1 + j];
						break;
					} else {
						System.err.println("Unknown option: " + args[i]);
						printUsage();
						LCCore.stop(true);
						return;
					}
				}
				boolean stop = true;
				if (groupId == null) System.err.println("Missing groupId parameter in command line");
				else if (artifactId == null) System.err.println("Missing artifactId parameter in command line");
				else if (version == null) System.err.println("Missing version parameter in command line");
				else stop = false;
				if (stop) {
					LCCore.stop(true);
					return;
				}
				
				// add local maven repository
				File dir = new File(System.getProperty("user.home") + "/.m2/repository");
				if (dir.exists()) {
					MavenLocalRepository repo = new MavenLocalRepository(dir, true, true);
					pomLoader.addRepository(repo);
					repositories.add(repo);
				}
				
				// load configuration file
				File cfgFile = null;
				for (LibrariesRepository repo : repositories) {
					cfgFile = repo.loadFileSync(groupId, artifactId, version, "lc-project", "xml");
					if (cfgFile != null)
						break;
				}
				if (cfgFile == null) {
					System.err.println("Configuration file does not exist");
					LCCore.stop(true);
					return;
				}
				
				if (splash != null) splash.setText("Reading application configuration");
				ApplicationConfiguration cfg;
				try { cfg = ApplicationConfiguration.load(cfgFile); }
				catch (Exception e) {
					System.err.println("Error reading configuration file " + cfgFile.getAbsolutePath());
					e.printStackTrace(System.err);
					LCCore.stop(true);
					return;
				}
				if (splash != null && cfg.name != null) splash.setApplicationName(cfg.name);
				File appDir = cfgFile.getParentFile();
				
				if (splash != null) splash.endInit();
				
				// setup properties
				if (cfg.properties.get(Application.PROPERTY_CONFIG_DIRECTORY) == null &&
					System.getProperty(Application.PROPERTY_CONFIG_DIRECTORY) == null)
					cfg.properties.put(Application.PROPERTY_CONFIG_DIRECTORY,
						System.getProperty("user.home") + "/.lc.apps/" + groupId + "/" + artifactId + "/cfg");
				if (cfg.properties.get(Application.PROPERTY_LOG_DIRECTORY) == null &&
					System.getProperty(Application.PROPERTY_LOG_DIRECTORY) == null)
					cfg.properties.put(Application.PROPERTY_LOG_DIRECTORY,
						System.getProperty("user.home") + "/.lc.apps/" + groupId + "/" + artifactId + "/log");
				
				List<Triple<String,String,String>> addPlugins = new LinkedList<>();
				if (plugins != null) {
					String[] list = plugins.split(";");
					for (String plugin : list) {
						plugin = plugin.trim();
						if (plugin.length() == 0) continue;
						int i = plugin.indexOf(':');
						if (i < 0) {
							addPlugins.add(new Triple<>(plugin, null, null));
							continue;
						}
						String pluginGroupId = plugin.substring(0, i);
						plugin = plugin.substring(i + 1);
						i = plugin.indexOf(':');
						if (i < 0) {
							addPlugins.add(new Triple<>(pluginGroupId, plugin, null));
							continue;
						}
						addPlugins.add(new Triple<>(pluginGroupId, plugin.substring(0, i), plugin.substring(i + 1)));
					}
				}
				DynamicLibrariesManager librariesManager =
					new DynamicLibrariesManager(null, splash, loaders, appDir, cfg, addPlugins);
				
				ISynchronizationPoint<Exception> start = Application.start(
					new Artifact(groupId, artifactId, new Version(version)),
					appParameters,
					cfg.properties,
					true,
					Executors.defaultThreadFactory(),
					librariesManager
				);
				start.block(0);
				if (start.hasError()) {
					start.getError().printStackTrace(System.err);
					LCCore.stop(true);
					return;
				}
				if (start.isCancelled()) {
					start.getCancelEvent().printStackTrace(System.err);
					LCCore.stop(true);
					return;
				}
		
				Task.Cpu<ISynchronizationPoint<Exception>, Exception> startApp = librariesManager.startApp();
				startApp.getOutput().block(0);
				if (!startApp.isSuccessful()) {
					if (startApp.isCancelled()) {
						System.err.println("Application cancelled:");
						startApp.getCancelEvent().printStackTrace(System.err);
					} else {
						System.err.println("Error while starting application:");
						startApp.getError().printStackTrace(System.err);
					}
					LCCore.stop(true);
				}
				ISynchronizationPoint<Exception> appClosed = startApp.getResult();
				if (appClosed != null) {
					appClosed.block(0);
					if (appClosed.isCancelled()) {
						System.err.println("Application cancelled:");
						appClosed.getCancelEvent().printStackTrace(System.err);
					} else if (appClosed.hasError()) {
						System.err.println("Error while running application:");
						appClosed.getError().printStackTrace(System.err);
					}
				}
				LCCore.stop(true);
			}
		});

	}
	
	/** Print usage in the console. */
	public static void printUsage() {
		System.out.println(
			"Usage: -groupId=<groupId> -artifactId=<artifactId> -version=<artifactVersion> [-maven-repository=<path>]");
	}
	
}
