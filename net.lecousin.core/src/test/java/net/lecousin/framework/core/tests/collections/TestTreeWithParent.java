package net.lecousin.framework.core.tests.collections;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.Tree;
import net.lecousin.framework.collections.Tree.Node;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestTreeWithParent extends LCCoreAbstractTest {

	@Test
	public void test() {
		Tree.WithParent<Integer> tree = new Tree.WithParent<>(null);
		Assert.assertEquals(0, tree.size());
		tree.add(Integer.valueOf(51));
		Assert.assertEquals(1, tree.size());
		Assert.assertEquals(1, tree.getElements().size());
		Assert.assertEquals(51, tree.getElements().get(0).intValue());
		Node<Integer> n = tree.get(Integer.valueOf(51));
		Assert.assertFalse(null == n);
		Assert.assertEquals(tree, ((Tree.WithParent<Integer>)n.getSubNodes()).getParent());
		Assert.assertEquals(51, n.getElement().intValue());
		Assert.assertEquals(0, n.getSubNodes().size());
		n = tree.get(0);
		Assert.assertFalse(null == n);
		Assert.assertNull(tree.get(Integer.valueOf(5151)));
		Assert.assertEquals(tree, ((Tree.WithParent<Integer>)n.getSubNodes()).getParent());
		Assert.assertEquals(51, n.getElement().intValue());
		Assert.assertEquals(0, n.getSubNodes().size());
		Assert.assertFalse(tree.remove(Integer.valueOf(5151)));
		Assert.assertTrue(tree.remove(Integer.valueOf(51)));
		Assert.assertEquals(0, tree.size());
		tree.add(Integer.valueOf(51));
		Assert.assertFalse(tree.removeInstance(Integer.valueOf(5151)));
		Assert.assertTrue(tree.removeInstance(tree.get(0).getElement()));
		Assert.assertEquals(0, tree.size());
		Assert.assertEquals(0, tree.getElements().size());
		Assert.assertEquals(0, tree.getNodes().size());
		Assert.assertFalse(tree.remove(Integer.valueOf(1)));
		Assert.assertFalse(tree.removeInstance(Integer.valueOf(1)));
		Assert.assertNull(tree.get(Integer.valueOf(1)));
	}
	
}
