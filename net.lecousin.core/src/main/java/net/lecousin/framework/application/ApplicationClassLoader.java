package net.lecousin.framework.application;

import java.net.URL;

import net.lecousin.framework.io.provider.IOProvider;

/**
 * ClassLoader dedicated to an application.
 */
public interface ApplicationClassLoader {

	/** Return the application. */
	Application getApplication();
	
	/** Return an IOProvider for the given application's resource. */
	IOProvider.Readable getIOProvider(String filename);
	
	/** Load a class. */
	Class<?> loadClass(String className) throws ClassNotFoundException;
	
	/** Search a resource. */
	URL getResource(String filename);
	
}
