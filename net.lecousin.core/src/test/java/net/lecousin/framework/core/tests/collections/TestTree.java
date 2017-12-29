package net.lecousin.framework.core.tests.collections;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.Tree;
import net.lecousin.framework.collections.Tree.Node;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestTree extends LCCoreAbstractTest {

	@Test
	public void test() {
		Tree<Integer> tree = new Tree<>();
		Assert.assertEquals(0, tree.size());
		tree.add(Integer.valueOf(51));
		Assert.assertEquals(1, tree.size());
		Node<Integer> n = tree.get(Integer.valueOf(51));
		Assert.assertFalse(null == n);
		Assert.assertEquals(51, n.getElement().intValue());
		Assert.assertEquals(0, n.getSubNodes().size());
		n = tree.get(0);
		Assert.assertFalse(null == n);
		Assert.assertEquals(51, n.getElement().intValue());
		Assert.assertEquals(0, n.getSubNodes().size());
		Assert.assertTrue(tree.remove(Integer.valueOf(51)));
		Assert.assertEquals(0, tree.size());
		tree.add(Integer.valueOf(51));
		Assert.assertTrue(tree.removeInstance(tree.get(0).getElement()));
		Assert.assertEquals(0, tree.size());
	}
	
}
