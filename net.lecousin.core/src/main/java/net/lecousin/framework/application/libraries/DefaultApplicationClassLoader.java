package net.lecousin.framework.application.libraries;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;

/**
 * Default implementation of ApplicationClassLoader.
 * It is a simple class loader, that keeps a reference to the application given in the constructor.
 */
public class DefaultApplicationClassLoader extends ApplicationClassLoader {
	
	/** Constructor. */
	public DefaultApplicationClassLoader(Application app) {
		super(DefaultApplicationClassLoader.class.getClassLoader());
		this.app = app;
	}
	
	private Application app;
	
	@Override
	public Application getApplication() {
		return app;
	}

}
