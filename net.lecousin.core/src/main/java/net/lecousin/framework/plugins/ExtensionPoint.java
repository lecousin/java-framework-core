package net.lecousin.framework.plugins;

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
	
}
