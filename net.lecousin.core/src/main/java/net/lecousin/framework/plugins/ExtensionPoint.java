package net.lecousin.framework.plugins;

import java.util.Collection;

/**
 * An extension point allows to register plug-ins.
 * Plug-ins are loaded at startup, by reading the declarations in META-INF/net.lecousin/plugins
 * @param <PluginClass> type of plugin
 */
public interface ExtensionPoint<PluginClass extends Plugin> {

	/** Class of plugin. */
	public Class<PluginClass> getPluginClass();
	
	/** Register a plugin (called at application startup). */
	public void addPlugin(PluginClass plugin);
	
	/** Signal that application has been fully loaded and no more plug-ins will be added. */
	public void allPluginsLoaded();
	
	public Collection<PluginClass> getPlugins();
	
	default public void printInfo(StringBuilder s) {
		s.append(getClass().getName()).append(":");
		for (PluginClass pi : getPlugins()) {
			s.append("\r\n\t- ").append(pi.getClass().getName());
		}
	}
	
}
