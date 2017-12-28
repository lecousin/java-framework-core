package net.lecousin.framework.core.tests.plugins;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.plugins.ExtensionPoint;
import net.lecousin.framework.plugins.Plugin;

public class AnExtensionPoint implements ExtensionPoint<AnExtensionPoint.MyPlugin> {

	public static class MyPlugin implements Plugin {
	}

	@Override
	public Class<MyPlugin> getPluginClass() {
		return MyPlugin.class;
	}
	
	private List<MyPlugin> plugins = new LinkedList<>();

	@Override
	public void addPlugin(MyPlugin plugin) {
		plugins.add(plugin);
	}

	@Override
	public void allPluginsLoaded() {
	}

	@Override
	public Collection<MyPlugin> getPlugins() {
		return plugins;
	}
	
}
