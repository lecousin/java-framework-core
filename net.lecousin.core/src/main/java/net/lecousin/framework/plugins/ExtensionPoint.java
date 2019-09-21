package net.lecousin.framework.plugins;

import java.util.Collection;

/**
 * An extension point allows to register plug-ins.
 * Plug-ins are loaded at startup, by reading the declarations in META-INF/net.lecousin/plugins
 * @param <PluginClass> type of plugin
 */
public interface ExtensionPoint<PluginClass extends Plugin> {

	/** Class of plugin. */
	Class<PluginClass> getPluginClass();
	
	/** Register a plugin (called at application startup). */
	void addPlugin(PluginClass plugin);
	
	/** Signal that application has been fully loaded and no more plug-ins will be added. */
	void allPluginsLoaded();

	/** Return the list of registered plugins. */
	Collection<PluginClass> getPlugins();
	
	/** Print information about this extension point to the given StringBuilder. */
	default void printInfo(StringBuilder s) {
		s.append(getClass().getName()).append(":");
		for (PluginClass pi : getPlugins()) {
			if (pi != null)
				s.append("\r\n\t- ").append(pi.getClass().getName());
			else
				s.append("\r\n\tNULL !!!");
		}
	}
	
}
