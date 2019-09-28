package net.lecousin.framework.plugins;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;

/**
 * Custom extension point. <br/>
 * A normal extension point loads its plug-ins using the files META-INF/net.lecousin/plugins present in the class path.<br/>
 * A Custom extension point can specify another file name and a custom loading.<br/>
 * It may be used as a bootstrap class for a library, as it will be automatically instantiated when its library is loaded.
 */
public interface CustomExtensionPoint {

	default String getPluginConfigurationFilePath() { return null; }
	
	/** Load configuration. */
	@SuppressWarnings("unused")
	default <T extends ClassLoader & ApplicationClassLoader> IAsync<Exception> loadPluginConfiguration(
		IO.Readable io, T libraryClassLoader, IAsync<?>... startOn
	) {
		return null;
	}
	
	/** Print information about this extension point to the given StringBuilder. */
	default void printInfo(StringBuilder s) {
		s.append(getClass().getName());
	}
	
	/** Return true if the instance of this extension point should be kept in memory. */
	boolean keepAfterInit();
	
}
