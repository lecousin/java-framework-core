package net.lecousin.framework.application.launcher;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.launcher.Launcher.CommandLineContext;
import net.lecousin.framework.util.CommandLine;

/** Plug-in to add functionalities while starting and loading application. */
public interface LauncherService {

	/** Add consumers for specific options. */
	void addCommandLineArgumentsConsumers(List<CommandLine.ArgumentsConsumer<CommandLineContext>> consumers);
	
	/** Print usage for specific options. */
	void printOptionsUsage();
	
	/** Check the command line, fill errors if needed, and return true if this service is activated.
	 * 
	 * @param context command line
	 * @param errors list of errors to fill
	 * @return true if the command line activate this service
	 */
	boolean checkCommandLineContext(CommandLineContext context, List<String> errors);
	
	/** Fill the given paths where to search for projects. */
	void getSearchProjectsPaths(CommandLineContext context, List<File> path);
	
	/** Return true if debug mode should be activated. */
	boolean activeDebugMode(CommandLineContext context);
	
}
