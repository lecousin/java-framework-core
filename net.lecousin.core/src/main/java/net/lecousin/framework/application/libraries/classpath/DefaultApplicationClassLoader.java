package net.lecousin.framework.application.libraries.classpath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;

/**
 * Default implementation of ApplicationClassLoader.
 * It is a simple class loader, that keeps a reference to the application given in the constructor.
 */
public class DefaultApplicationClassLoader extends URLClassLoader implements ApplicationClassLoader {
	
	static {
		ClassLoader.registerAsParallelCapable();
	}
	
	/** Constructor. */
	public DefaultApplicationClassLoader(Application app, File[] additionalClassPath) {
		super(getURLs(additionalClassPath), DefaultApplicationClassLoader.class.getClassLoader());
		this.app = app;
	}
	
	private Application app;
	
	@Override
	public Application getApplication() {
		return app;
	}
	
	private static URL[] getURLs(File[] files) {
		if (files == null)
			return new URL[0];
		URL[] urls = new URL[files.length];
		for (int i = 0; i < files.length; ++i)
			try {
				urls[i] = files[i].toURI().toURL();
			} catch (MalformedURLException e) {
				// should not happen
			}
		return urls;
	}

}
