package net.lecousin.framework.application.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.application.launcher.Launcher.CommandLineContext;
import net.lecousin.framework.util.CommandLine;
import net.lecousin.framework.util.CommandLine.ArgumentsConsumer;

/**
 * Application launcher in a development environment.
 */
@SuppressWarnings({
	"squid:S106", // print to console
})
public class DevLauncher implements LauncherService {
	
	public static final String EXTENSION_PROJECTS = "dev.projects";

	private static class ProjectsConsumer implements CommandLine.ArgumentsConsumer<CommandLineContext> {
		@Override
		public boolean matches(String arg, CommandLineContext context) {
			return arg.startsWith("-projects=");
		}
		
		@Override
		public int consume(String[] args, int index, CommandLineContext context) {
			if (context.extensions.containsKey(EXTENSION_PROJECTS))
				throw new IllegalArgumentException("Option -projects cannot be specified several times");
			context.extensions.put(EXTENSION_PROJECTS, args[index].substring(10));
			return 1;
		}
	}

	@Override
	public void addCommandLineArgumentsConsumers(List<ArgumentsConsumer<CommandLineContext>> consumers) {
		consumers.add(new ProjectsConsumer());
	}

	@Override
	public void printOptionsUsage() {
		System.out.println("[-projects=<paths>]                Paths to search for development projects");
		System.out.println("                                   using operating system separator");
	}

	@Override
	public boolean checkCommandLineContext(CommandLineContext context, List<String> errors) {
		if (!context.extensions.containsKey(EXTENSION_PROJECTS))
			return false;
		String[] s = ((String)context.extensions.get(EXTENSION_PROJECTS)).split(File.pathSeparator);
		ArrayList<File> devPaths = new ArrayList<>(s.length);
		for (String path : s) {
			File dir = new File(path);
			if (!dir.exists())
				System.err.println("Development path " + path + " does not exist");
			else
				devPaths.add(dir);
		}
		context.extensions.put(EXTENSION_PROJECTS, devPaths);
		return !devPaths.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void getSearchProjectsPaths(CommandLineContext context, List<File> path) {
		path.addAll((ArrayList<File>)context.extensions.get(EXTENSION_PROJECTS));
	}
	
	@Override
	public boolean activeDebugMode(CommandLineContext context) {
		return true;
	}
	
}
