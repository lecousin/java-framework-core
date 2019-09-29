package net.lecousin.core.loaders.maven;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.launcher.Launcher.CommandLineContext;
import net.lecousin.framework.application.launcher.LauncherService;
import net.lecousin.framework.util.CommandLine;
import net.lecousin.framework.util.CommandLine.ArgumentsConsumer;
import net.lecousin.framework.util.SystemEnvironment;

/** Plug-in to use maven repositories and pom files to launch and load application. */
@SuppressWarnings({
	"squid:S106", // print to console
})
public class LauncherUsingMaven implements LauncherService {

	public static final String MAVEN_POM_LOADER_EXTENSION = "maven.pomLoader";
	
	private static class MavenRepositoryOptionConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
		@Override
		public boolean matches(String arg, CommandLineContext context) {
			return arg.startsWith("-maven-repository=");
		}
		
		@Override
		public int consume(String[] args, int index, CommandLineContext context) {
			String path = args[index].substring(18);
			File dir = new File(path);
			if (dir.exists()) {
				MavenLocalRepository repo = new MavenLocalRepository(dir, true, true);
				context.repositories.add(repo);
				MavenPOMLoader pomLoader = (MavenPOMLoader)context.extensions.get(MAVEN_POM_LOADER_EXTENSION);
				if (pomLoader == null) {
					pomLoader = new MavenPOMLoader();
					context.extensions.put(MAVEN_POM_LOADER_EXTENSION, pomLoader);
				}
				pomLoader.addRepository(repo);
			}
			return 1;
		}
	}
	
	@Override
	public void addCommandLineArgumentsConsumers(List<ArgumentsConsumer<CommandLineContext>> consumers) {
		consumers.add(new MavenRepositoryOptionConsumer());
	}
	
	@Override
	public void printOptionsUsage() {
		System.out.println("[-maven-repository=<path>]*        Path to a Maven repository to look for");
		System.out.println("                                   artifacts. The option can be specified");
		System.out.println("                                   several times for several repositories");
	}
	
	@Override
	public boolean checkCommandLineContext(CommandLineContext context, List<String> errors) {
		if (!context.extensions.containsKey(MAVEN_POM_LOADER_EXTENSION))
			context.extensions.put(MAVEN_POM_LOADER_EXTENSION, new MavenPOMLoader());
		MavenPOMLoader pomLoader = (MavenPOMLoader)context.extensions.get(MAVEN_POM_LOADER_EXTENSION);
		context.loaders.add(pomLoader);
		
		// add local maven repository
		File settings = new File(System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME) + "/.m2/settings.xml");
		String localRepo = System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME) + "/.m2/repository";
		if (settings.exists()) {
			try {
				MavenSettings ms = MavenSettings.load(settings);
				if (ms.getLocalRepository() != null)
					localRepo = ms.getLocalRepository();
			} catch (Exception e) {
				System.err.println("Error reading Maven settings.xml");
				e.printStackTrace(System.err);
			}
		}
		File dir = new File(localRepo);
		if (dir.exists()) {
			MavenLocalRepository repo = new MavenLocalRepository(dir, true, true);
			pomLoader.addRepository(repo);
			context.repositories.add(repo);
		}

		return true;
	}

	@Override
	public void getSearchProjectsPaths(CommandLineContext context, List<File> path) {
		// nothing
	}
	
	@Override
	public boolean activeDebugMode(CommandLineContext context) {
		return false;
	}
}
