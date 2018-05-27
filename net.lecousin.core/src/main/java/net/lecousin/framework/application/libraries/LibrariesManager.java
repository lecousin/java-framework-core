package net.lecousin.framework.application.libraries;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;

/** Allows to load libraries and get information about loaded ones. */
public interface LibrariesManager {

	/** This method is automatically called on application startup so this libraries manager
	 * can initialize and load libraries as needed for the given application.
	 */
	<T extends ClassLoader & ApplicationClassLoader> T start(Application app);
	
	/** Return a synchronization point which is blocked until this libraries manager has been initialized
	 * and has loaded all required libraries. */
	ISynchronizationPoint<Exception> onLibrariesLoaded();
	
	/** Open a resource or return null if it does not exist. */
	IO.Readable getResource(String path, byte priority);

	/** Return the list of libraries loaded. Each File may be a file in case of a JAR, or a directory. */
	List<File> getLibrariesLocations();
	
}
