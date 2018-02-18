package net.lecousin.framework.core.tests.plugins;

import java.util.Collection;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.tests.plugins.AnExtensionPoint.MyPlugin;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.plugins.Plugin;

import org.junit.Assert;
import org.junit.Test;

public class TestPlugins extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testPlugins() {
		AnExtensionPoint ep = ExtensionPoints.getExtensionPoint(AnExtensionPoint.class);
		Assert.assertFalse(ep == null);
		Collection<MyPlugin> plugins = ep.getPlugins();
		Assert.assertEquals(1, plugins.size());
		Assert.assertTrue(plugins.iterator().next() instanceof APlugin);
		ACustomExtensionPoint custom = ExtensionPoints.getCustomExtensionPoint(ACustomExtensionPoint.class);
		Assert.assertNotNull(custom);
		custom.getPluginConfigurationFilePath();
		custom.loadPluginConfiguration(null, null);
		Assert.assertNull(ExtensionPoints.getCustomExtensionPoint(FakeCustom.class));
		Assert.assertNull(ExtensionPoints.getExtensionPoint(FakePoint.class));
		// for coverage
		for (ExtensionPoint<?> e : ExtensionPoints.getExtensionPoints()) {
			e.getPluginClass();
		}
		ExtensionPoints.logRemainingPlugins();
		
		ACustomExtensionPointWithFile c = ExtensionPoints.getCustomExtensionPoint(ACustomExtensionPointWithFile.class);
		Assert.assertNotNull(c);
		Assert.assertEquals("The test is successful", c.pluginContent);
	}
	
	public static class FakeCustom implements CustomExtensionPoint {
		@Override
		public boolean keepAfterInit() {
			return false;
		}
	}
	
	public static class FakePoint implements ExtensionPoint<Plugin> {
		@Override
		public Class<Plugin> getPluginClass() {
			return null;
		}

		@Override
		public void addPlugin(Plugin plugin) {
		}

		@Override
		public void allPluginsLoaded() {
		}

		@Override
		public Collection<Plugin> getPlugins() {
			return null;
		}
	}
	
}
