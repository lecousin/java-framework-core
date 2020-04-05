package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.AbstractAttributesContainer;

import org.junit.Assert;
import org.junit.Test;

public class TestAttributesContainer extends LCCoreAbstractTest {

	@Test
	public void test() {
		AbstractAttributesContainer container = new AbstractAttributesContainer() {
		};
		Assert.assertNull(container.getAttribute("a"));
		Assert.assertFalse(container.hasAttribute("a"));
		container.setAttribute("a", "b");
		Assert.assertTrue(container.hasAttribute("a"));
		Assert.assertEquals("b", container.getAttribute("a"));
		Assert.assertTrue(container.hasAttribute("a"));
		Assert.assertEquals("b", container.removeAttribute("a"));
		Assert.assertFalse(container.hasAttribute("a"));
		Assert.assertNull(container.getAttribute("a"));
	}
	
}
