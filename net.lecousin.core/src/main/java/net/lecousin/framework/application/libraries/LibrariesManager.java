package net.lecousin.framework.application.libraries;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.progress.WorkProgress;

/** Allows to load libraries and get information about loaded ones. */
public interface LibrariesManager {

	/** This method is automatically called on application startup so this libraries manager
	 * can initialize and load libraries as needed for the given application.
	 */
	ApplicationClassLoader start(Application app);
	
	/** Return a synchronization point which is blocked until this libraries manager has been initialized and has loaded any required library. */
	ISynchronizationPoint<Exception> onLibrariesLoaded();
	
	/** Return true if this libraries manager has the capability to dynamically load libraries after startup. */
	boolean canLoadNewLibraries();
	
	/** Load a new library. */
	AsyncWork<Library,Exception> loadNewLibrary(
		String groupId, String artifactId, VersionSpecification version, boolean optional,
		byte priority, WorkProgress progress, long work);

	/** Return a loaded library, or null if not loaded. */
	Library getLibrary(String groupId, String artifactId);
	
	/** Return the loaded library for the given class loader, or null if no specific library matches this class loader. */
	Library getLibrary(ClassLoader cl);
	
	/** Return the loaded library for the given class, or null if no specific library matches this class. */
	default Library getLibrary(Class<?> clazz) { return getLibrary(clazz.getClassLoader()); }
	
	/** Open a resource from a specific library if possible, or return null if it does not exist. */
	IO.Readable getResource(String groupId, String artifactId, String path, byte priority);
	
	/** Open a resource or return null if it does not exist. */
	default IO.Readable getResource(String path, byte priority) {
		return getResource(null, null, path, priority);
	}
	
	/** Open a resource from a specific library if possible, or return null if it does not exist. */
	IO.Readable getResourceFrom(ClassLoader cl, String path, byte priority);

	/** Open a resource from a specific library if possible, or return null if it does not exist. */
	default IO.Readable getResourceFrom(Class<?> clazz, String path, byte priority) {
		return getResourceFrom(clazz.getClassLoader(), path, priority);
	}

	/** Return the list of libraries loaded. Each File may be a file in case of a JAR, or a directory. */
	List<File> getLibrariesLocations();
	
}
