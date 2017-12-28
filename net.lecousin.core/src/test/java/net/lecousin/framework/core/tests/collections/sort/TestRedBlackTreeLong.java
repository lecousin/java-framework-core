package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.RedBlackTreeLong.Node;
import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.core.test.collections.sort.TestSortedAssociatedWithLong;

public class TestRedBlackTreeLong extends TestSortedAssociatedWithLong {

	@Override
	protected Sorted.AssociatedWithLong<Object> createSorted() {
		return new RedBlackTreeLong<Object>();
	}
	
	@Override
	protected void check(Sorted.AssociatedWithLong<Object> sorted, TreeSet<Long> order) {
		super.check(sorted, order);
		RedBlackTreeLong<Object> tree = (RedBlackTreeLong<Object>)sorted;
		if (!tree.isEmpty()) {
			Node<Object> n = tree.getMin();
			Assert.assertFalse(n == null);
			Iterator<Long> it = order.iterator();
			Assert.assertEquals(it.next().longValue(), n.getValue());
			Node<Object> n2 = n;
			while (it.hasNext()) {
				long val = n2.getValue();
				n2 = tree.getNext(n2);
				Assert.assertFalse(n2 == null);
				Assert.assertEquals(it.next().longValue(), n2.getValue());
				Assert.assertTrue(n2 == tree.getNext(val));
			}
			n2 = tree.getNext(n2);
			Assert.assertTrue(n2 == null);
			n2 = tree.getNext(tree.getMax().getValue());
			Assert.assertTrue(n2 == null);

			n = tree.getMax();
			Assert.assertFalse(n == null);
			it = order.descendingIterator();
			Assert.assertEquals(it.next().longValue(), n.getValue());
			n2 = n;
			while (it.hasNext()) {
				long val = n2.getValue();
				n2 = tree.getPrevious(n2);
				Assert.assertFalse(n2 == null);
				Assert.assertEquals(it.next().longValue(), n2.getValue());
				Assert.assertTrue(n2 == tree.getPrevious(val));
			}
			n2 = tree.getPrevious(n2);
			Assert.assertTrue(n2 == null);
			n2 = tree.getPrevious(tree.getMin().getValue());
			Assert.assertTrue(n2 == null);
		} else {
			Node<Object> n = tree.getMin();
			Assert.assertTrue(n == null);
			n = tree.getMax();
			Assert.assertTrue(n == null);
		}
	}
	
	@Test
	public void tests() {
		RedBlackTreeLong<Object> tree = new RedBlackTreeLong<Object>();
		Long o = Long.valueOf(1664);
		tree.add(51, o);
		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsInstance(51, o));
		tree.removeKey(51);
		Assert.assertEquals(0, tree.size());
		Assert.assertFalse(tree.containsInstance(51, o));
		tree.add(51, o);
		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsInstance(51, o));
		tree.removeInstance(51, o);
		Assert.assertEquals(0, tree.size());
		Assert.assertFalse(tree.containsInstance(51, o));
		
		for (int i = 0; i < 100; ++i)
			tree.add(i, o);
		Assert.assertEquals(100, tree.size());
		tree.removeKey(10);
		Assert.assertEquals(99, tree.size());
		tree.removeKey(20);
		Assert.assertEquals(98, tree.size());
		tree.removeKey(3);
		Assert.assertEquals(97, tree.size());
		tree.removeKey(0);
		Assert.assertEquals(96, tree.size());
		tree.removeKey(99);
		Assert.assertEquals(95, tree.size());
		tree.removeKey(80);
		Assert.assertEquals(94, tree.size());
		tree.removeInstance(11, o);
		Assert.assertEquals(93, tree.size());
		tree.removeInstance(21, o);
		Assert.assertEquals(92, tree.size());
		tree.removeInstance(4, o);
		Assert.assertEquals(91, tree.size());
		tree.removeInstance(1, o);
		Assert.assertEquals(90, tree.size());
		tree.removeInstance(98, o);
		Assert.assertEquals(89, tree.size());
		tree.removeInstance(79, o);
		Assert.assertEquals(88, tree.size());
	}

}
