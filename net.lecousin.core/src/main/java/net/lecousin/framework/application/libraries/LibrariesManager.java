package net.lecousin.framework.application.libraries;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;

/** Allows to load libraries and get information about loaded ones. */
public interface LibrariesManager {

	/** This method is automatically called on application startup so this libraries manager
	 * can initialize and load libraries as needed for the given application.
	 */
	ApplicationClassLoader start(Application app);
	
	/** Return a synchronization point which is blocked until this libraries manager has been initialized
	 * and has loaded all required libraries. */
	IAsync<LibraryManagementException> onLibrariesLoaded();
	
	/** Open a resource or return null if it does not exist. */
	IO.Readable getResource(String path, Priority priority);

	/** Return the list of libraries loaded. Each File may be a file in case of a JAR, or a directory. */
	List<File> getLibrariesLocations();
	
	/** Go through each library to scan its content. */
	void scanLibraries(String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner);
	
}
