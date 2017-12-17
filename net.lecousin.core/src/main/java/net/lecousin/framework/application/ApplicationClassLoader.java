package net.lecousin.framework.application;

/**
 * ClassLoader dedicated to an application.
 * Such a ClassLoader must be used in a multi-application environment.
 */
public abstract class ApplicationClassLoader extends ClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}
	
	public ApplicationClassLoader() {
	}
	
	public ApplicationClassLoader(ClassLoader parent) {
		super(parent);
	}
	
	/** Return the application. */
	public abstract Application getApplication();
	
}
