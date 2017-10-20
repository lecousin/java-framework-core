package net.lecousin.framework.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.lecousin.framework.adapter.AdapterRegistry;
import net.lecousin.framework.io.serialization.annotations.AnnotationToRule;
import net.lecousin.framework.locale.LocaleExtensionPoint;
import net.lecousin.framework.math.IntegerUnit;

/** Extension points registry. */
public final class ExtensionPoints {

	private ExtensionPoints() {
	}
	
	private static ArrayList<ExtensionPoint<?>> points = new ArrayList<>();
	private static ArrayList<Plugin> waitingPlugins = new ArrayList<>();
	private static ArrayList<CustomExtensionPoint> customs = new ArrayList<>();
	
	static {
		points.add(new AdapterRegistry());
		points.add(new AnnotationToRule());
		points.add(new IntegerUnit.ConverterRegistry());
		customs.add(new LocaleExtensionPoint());
	}
	
	/** Add an extension point. */
	@SuppressWarnings("unchecked")
	public static <T extends Plugin> void add(ExtensionPoint<T> point) {
		ArrayList<Plugin> plugins = new ArrayList<>();
		synchronized (points) {
			points.add(point);
			for (Iterator<Plugin> it = waitingPlugins.iterator(); it.hasNext(); ) {
				Plugin pi = it.next();
				if (point.getPluginClass().isAssignableFrom(pi.getClass())) {
					plugins.add(pi);
					it.remove();
				}
			}
		}
		for (Plugin pi : plugins)
			point.addPlugin((T)pi);
	}
	
	/** Add a custom extension point. */
	public static void add(CustomExtensionPoint custom) {
		synchronized (customs) {
			customs.add(custom);
		}
	}

	/** Add a plug-in for the given extension point class name. */
	@SuppressWarnings("unchecked")
	public static void add(String epClassName, Plugin pi) {
		ExtensionPoint<Plugin> ep = null;
		synchronized (points) {
			for (ExtensionPoint<?> point : points)
				if (point.getClass().getName().equals(epClassName)) {
					ep = (ExtensionPoint<Plugin>)point;
					break;
				}
			if (ep == null) {
				waitingPlugins.add(pi);
				return;
			}
		}
		ep.addPlugin(pi);
	}
	
	/** Call the method allPluginsLoaded on every extension point. */
	public static void allPluginsLoaded() {
		synchronized (points) {
			for (ExtensionPoint<?> ep : points)
				ep.allPluginsLoaded();
		}
	}
	
	/** Return all registered custom extension points. */
	public static List<CustomExtensionPoint> getCustomExtensionPoints() {
		synchronized (customs) {
			return new ArrayList<>(customs);
		}
	}
	
	/** Print to the error console the plugins that are available without their corresponding extension point. */
	public static void logRemainingPlugins() {
		synchronized (points) {
			for (Plugin pi : waitingPlugins)
				System.err.println("Plugin ignored because extension point is not loaded: " + pi.getClass().getName());
		}
	}
	
}
