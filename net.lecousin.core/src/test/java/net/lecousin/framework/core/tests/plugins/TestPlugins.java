package net.lecousin.framework.core.tests.plugins;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.tests.plugins.AnExtensionPoint.MyPlugin;
import net.lecousin.framework.plugins.ExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;

public class TestPlugins extends LCCoreAbstractTest {

	@Test
	public void testPlugins() {
		AnExtensionPoint ep = ExtensionPoints.getExtensionPoint(AnExtensionPoint.class);
		Assert.assertFalse(ep == null);
		Collection<MyPlugin> plugins = ep.getPlugins();
		Assert.assertEquals(1, plugins.size());
		Assert.assertTrue(plugins.iterator().next() instanceof APlugin);
		Assert.assertFalse(null == ExtensionPoints.getCustomExtensionPoint(ACustomExtensionPoint.class));
		// for coverage
		for (ExtensionPoint<?> e : ExtensionPoints.getExtensionPoints()) {
			e.getPluginClass();
		}
	}
	
}
