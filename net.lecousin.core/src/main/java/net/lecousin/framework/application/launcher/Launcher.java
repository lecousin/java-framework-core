package net.lecousin.framework.application.launcher;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationBootstrapException;
import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.SplashScreen;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.util.CommandLine;
import net.lecousin.framework.util.SystemEnvironment;
import net.lecousin.framework.util.Triple;

/** Class to launch an application. */
@SuppressWarnings({
	"squid:S106", // print to console
})
public class Launcher {
	
	/** Default command line context. */
	@SuppressWarnings("squid:ClassVariableVisibilityCheck")
	public static class CommandLineContext {
		public String groupId;
		public String artifactId;
		public String version;
		public String plugins;
		public String config;
		public String[] appParameters = new String[0];
		public List<LibraryDescriptorLoader> loaders = new LinkedList<>();
		public List<LibrariesRepository> repositories = new LinkedList<>();
		public Map<String, Object> extensions = new HashMap<>();

		/** Consumer for option groupId. */
		public static class GroupIdConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.startsWith("-groupId=");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				if (context.groupId != null)
					throw new IllegalArgumentException("Option -groupId cannot be specified several times");
				context.groupId = args[index].substring(9);
				return 1;
			}
		}
		
		/** Consumer for option artifactId. */
		public static class ArtifactIdConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.startsWith("-artifactId=");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				if (context.artifactId != null)
					throw new IllegalArgumentException("Option -artifactId cannot be specified several times");
				context.artifactId = args[index].substring(12);
				return 1;
			}
		}
		
		/** Consumer for option version. */
		public static class VersionConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.startsWith("-version=");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				if (context.version != null)
					throw new IllegalArgumentException("Option -version cannot be specified several times");
				context.version = args[index].substring(9);
				return 1;
			}
		}
		
		/** Consumer for option plugins. */
		public static class PluginsConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.startsWith("-plugins=");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				if (context.plugins != null)
					throw new IllegalArgumentException("Option -plugins cannot be specified several times");
				context.plugins = args[index].substring(9);
				return 1;
			}
		}
		
		/** Consumer for option config. */
		public static class ConfigConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.startsWith("-config=");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				if (context.config != null)
					throw new IllegalArgumentException("Option -config cannot be specified several times");
				context.config = args[index].substring(8);
				return 1;
			}
		}

		/** Consumer for option parameters. */
		public static class AppParametersConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
			@Override
			public boolean matches(String arg, CommandLineContext context) {
				return arg.equals("-parameters");
			}
			
			@Override
			public int consume(String[] args, int index, CommandLineContext context) {
				context.appParameters = new String[args.length - index - 1];
				for (int j = 0; j < context.appParameters.length; ++j)
					context.appParameters[j] = args[index + 1 + j];
				return args.length - index;
			}
		}
	
	}
	
	private static SplashScreen createSplashScreen() {
		if (!GraphicsEnvironment.isHeadless() && !"true".equals(System.getProperty("nosplash")))
			return new SplashScreen(true);
		return null;
	}

	@SuppressWarnings({
		"squid:S106", // print to console
	})
	private static boolean checkCommandLineContext(CommandLineContext context, List<LauncherService> services) {
		List<String> errors = new LinkedList<>();
		checkPresent(context.groupId, "groupId", errors);
		checkPresent(context.artifactId, "artifactId", errors);
		checkPresent(context.version, "version", errors);
		List<LauncherService> allServices = new ArrayList<>(services);
		for (Iterator<LauncherService> it = services.iterator(); it.hasNext(); )
			if (!it.next().checkCommandLineContext(context, errors))
				it.remove();
		if (errors.isEmpty())
			return true;
		for (String e : errors)
			System.err.println(e);
		printUsage(allServices);
		LCCore.stop(true);
		return false;
	}

	/** Utility method to check if an option is present. */
	public static void checkPresent(String s, String optionName, List<String> errors) {
		if (s == null || s.trim().length() == 0)
			errors.add("Missing option " + optionName + " in command line");
	}
	
	
	private static void printUsage(List<LauncherService> services) {
		System.out.println("Options:");
		System.out.println(" -groupId=<app groupId>            Mandatory: groupId of the application");
		System.out.println(" -artifactId=<app artifactId>      Mandatory: artifactId of the application");
		System.out.println(" -version=<app version>            Mandatory: application's artifact version");
		System.out.println("[-plugins=<plugins>]               Plug-in artifacts to load separated by a");
		System.out.println("                                   semi-colon in the following format:");
		System.out.println("                                   groupId[:artifactId[:version]]");
		System.out.println("[-config=<path>]                   Path to the project.xml file. If not");
		System.out.println("                                   specified it is searched automatically");
		for (LauncherService service : services)
			service.printOptionsUsage();
		System.out.println("[-parameters <...>]                all following parameters are passed to the");
		System.out.println("                                   application");
	}
	
	@SuppressWarnings("unchecked")
	private static CommandLineContext parseCommandLine(String[] args, List<LauncherService> services) {
		try {
			CommandLineContext cmdContext = new CommandLineContext();
			List<CommandLine.ArgumentsConsumer<CommandLineContext>> consumers = new LinkedList<>();
			consumers.add(new CommandLineContext.GroupIdConsumer());
			consumers.add(new CommandLineContext.ArtifactIdConsumer());
			consumers.add(new CommandLineContext.VersionConsumer());
			consumers.add(new CommandLineContext.PluginsConsumer());
			consumers.add(new CommandLineContext.ConfigConsumer());
			for (LauncherService service : services)
				service.addCommandLineArgumentsConsumers(consumers);
			
			CommandLine.parse(args, cmdContext, new CommandLine.ArgumentsConsumer[][] {
				consumers.toArray(new CommandLine.ArgumentsConsumer[consumers.size()]),
				new CommandLine.ArgumentsConsumer[] {
					new CommandLineContext.AppParametersConsumer()
				}
			});
			return cmdContext;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			printUsage(services);
			LCCore.stop(true);
			return null;
		}
	}
	
	private static File getConfigurationFile(CommandLineContext cmdContext) {
		File cfgFile = null;
		if (cmdContext.config != null) {
			cfgFile = new File(cmdContext.config);
			if (!cfgFile.exists() || !cfgFile.isFile()) {
				System.err.println("Configuration file does not exist");
				LCCore.stop(true);
				return null;
			}
		} else {
			for (LibrariesRepository repo : cmdContext.repositories) {
				cfgFile = repo.loadFileSync(
					cmdContext.groupId, cmdContext.artifactId, cmdContext.version, "lc-project", "xml");
				if (cfgFile != null)
					break;
			}
			if (cfgFile == null) {
				System.err.println("Configuration file not found");
				LCCore.stop(true);
				return null;
			}
		}
		return cfgFile;
	}
	
	private static ApplicationConfiguration loadConfiguration(File cfgFile) {
		ApplicationConfiguration cfg;
		try { cfg = ApplicationConfiguration.load(cfgFile); }
		catch (Exception e) {
			System.err.println("Error reading configuration file " + cfgFile.getAbsolutePath());
			e.printStackTrace(System.err);
			LCCore.stop(true);
			return null;
		}
		return cfg;
	}
	
	private static void setupProperties(CommandLineContext cmdContext, ApplicationConfiguration cfg) {
		// config directory
		if (cfg.getProperties().get(Application.PROPERTY_CONFIG_DIRECTORY) == null &&
			System.getProperty(Application.PROPERTY_CONFIG_DIRECTORY) == null)
			cfg.getProperties().put(Application.PROPERTY_CONFIG_DIRECTORY,
				System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME)
				+ "/.lc.apps/" + cmdContext.groupId + "/" + cmdContext.artifactId + "/cfg");
		
		// log directory
		if (cfg.getProperties().get(Application.PROPERTY_LOG_DIRECTORY) == null &&
			System.getProperty(Application.PROPERTY_LOG_DIRECTORY) == null)
			cfg.getProperties().put(Application.PROPERTY_LOG_DIRECTORY,
				System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME)
				+ "/.lc.apps/" + cmdContext.groupId + "/" + cmdContext.artifactId + "/log");
	}
	
	private static List<Triple<String,String,String>> parsePlugins(String plugins) {
		List<Triple<String,String,String>> addPlugins = new LinkedList<>();
		if (plugins != null) {
			String[] list = plugins.split(";");
			for (String plugin : list) {
				plugin = plugin.trim();
				if (plugin.length() == 0) continue;
				int i = plugin.indexOf(':');
				if (i < 0) {
					addPlugins.add(new Triple<>(plugin, null, null));
				} else {
					String pluginGroupId = plugin.substring(0, i);
					plugin = plugin.substring(i + 1);
					i = plugin.indexOf(':');
					if (i < 0) {
						addPlugins.add(new Triple<>(pluginGroupId, plugin, null));
					} else {
						addPlugins.add(new Triple<>(pluginGroupId, plugin.substring(0, i), plugin.substring(i + 1)));
					}
				}
			}
		}
		return addPlugins;
	}
	
	private static boolean startApplication(
		CommandLineContext cmdContext, ApplicationConfiguration cfg, boolean debugMode, LibrariesManager librariesManager
	) {
		IAsync<ApplicationBootstrapException> start = Application.start(
			new Artifact(cmdContext.groupId, cmdContext.artifactId, new Version(cmdContext.version)),
			cmdContext.appParameters,
			cfg.getProperties(),
			debugMode,
			Executors.defaultThreadFactory(),
			librariesManager,
			null
		);

		start.block(0);
		if (start.hasError()) {
			start.getError().printStackTrace(System.err);
			LCCore.stop(true);
			return false;
		}
		if (start.isCancelled()) {
			start.getCancelEvent().printStackTrace(System.err);
			LCCore.stop(true);
			return false;
		}
		return true;
	}
	
	private static IAsync<Exception> launchApplication(DynamicLibrariesManager librariesManager) {
		Task.Cpu<IAsync<Exception>, ApplicationBootstrapException> startApp = librariesManager.startApp();
		startApp.getOutput().block(0);
		if (!startApp.isSuccessful()) {
			if (startApp.isCancelled()) {
				System.err.println("Application cancelled:");
				startApp.getCancelEvent().printStackTrace(System.err);
			} else {
				System.err.println("Error while starting application:");
				startApp.getError().printStackTrace(System.err);
			}
			return null;
		}
		return startApp.getResult();
	}
	
	private static void start(final SplashScreen splash, final String[] args) {
		// load plugins for launcher
		List<LauncherService> services = new LinkedList<>();
		CollectionsUtil.addAll(services, ServiceLoader.load(LauncherService.class).iterator());

		// parse command line
		CommandLineContext cmdContext = parseCommandLine(args, services);
		if (cmdContext == null) return;
		if (!checkCommandLineContext(cmdContext, services))
			return;
		
		for (LauncherService service : services)
			System.out.println("Activated launcher service: " + service.getClass().getSimpleName());
		
		// search configuration file
		File cfgFile = getConfigurationFile(cmdContext);
		if (cfgFile == null) return;
		if (splash != null) splash.setText("Reading application configuration");
		
		// load configuration
		ApplicationConfiguration cfg = loadConfiguration(cfgFile);
		if (cfg == null) return;
		if (splash != null && cfg.getName() != null) splash.setApplicationName(cfg.getName());
		File appDir = cfgFile.getParentFile();
		
		if (splash != null) splash.endInit();
		
		// setup properties
		setupProperties(cmdContext, cfg);

		// parse plugins from command line
		List<Triple<String,String,String>> addPlugins = parsePlugins(cmdContext.plugins);
		
		// get info from services
		List<File> searchProjectsPaths = new LinkedList<>();
		boolean debugMode = false;
		for (LauncherService service : services) {
			service.getSearchProjectsPaths(cmdContext, searchProjectsPaths);
			debugMode |= service.activeDebugMode(cmdContext);
		}
		if (searchProjectsPaths.isEmpty())
			searchProjectsPaths = null;

		// start dynamic libraries manager
		DynamicLibrariesManager librariesManager =
			new DynamicLibrariesManager(searchProjectsPaths, splash, cmdContext.loaders, appDir, cfg, addPlugins);
		
		// start application framework
		if (!startApplication(cmdContext, cfg, debugMode, librariesManager))
			return;

		// launch application
		IAsync<Exception> appClosed = launchApplication(librariesManager);
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

	/** Entry point. */
	public static void main(String[] args) {
		// start the splash screen as soon as possible
		final SplashScreen splash = createSplashScreen();

		//-XstartOnFirstThread
		LCCore.keepMainThread(() -> start(splash, args));
	}
	
}
