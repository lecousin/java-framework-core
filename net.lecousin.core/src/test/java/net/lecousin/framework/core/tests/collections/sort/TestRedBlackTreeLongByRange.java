package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.RedBlackTreeLong.Node;
import net.lecousin.framework.collections.sort.RedBlackTreeLongByRange;
import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.core.test.collections.sort.TestSortedAssociatedWithLong;

public class TestRedBlackTreeLongByRange extends TestSortedAssociatedWithLong {

	@Override
	protected Sorted.AssociatedWithLong<Object> createSorted() {
		return new RedBlackTreeLongByRange<Object>(50);
	}
	
	@Override
	protected void check(Sorted.AssociatedWithLong<Object> sorted, TreeSet<Long> order) {
		super.check(sorted, order);
		RedBlackTreeLongByRange<Object> tree = (RedBlackTreeLongByRange<Object>)sorted;
		if (!tree.isEmpty()) {
			Node<Object> n = tree.getMin();
			Assert.assertFalse(n == null);
			Iterator<Long> it = order.iterator();
			Assert.assertEquals(it.next().longValue(), n.getValue());

			n = tree.getMax();
			Assert.assertFalse(n == null);
			it = order.descendingIterator();
			Assert.assertEquals(it.next().longValue(), n.getValue());
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
		Assert.assertNull(tree.getPrevious(51));
		Assert.assertNull(tree.getPrevious(50));
		Assert.assertNull(tree.getPrevious(52));
		Assert.assertNull(tree.getPrevious(tree.get(51)));
		Assert.assertNull(tree.getPrevious(tree.get(50)));
		Assert.assertNull(tree.getPrevious(tree.get(52)));
		Assert.assertNull(tree.getNext(51));
		Assert.assertNull(tree.getNext(50));
		Assert.assertNull(tree.getNext(52));
		Assert.assertNull(tree.getNext(tree.get(51)));
		Assert.assertNull(tree.getNext(tree.get(50)));
		Assert.assertNull(tree.getNext(tree.get(52)));
		Assert.assertTrue(tree.contains(51, o));
		tree.removeKey(51);
		Assert.assertEquals(0, tree.size());
		Assert.assertFalse(tree.containsInstance(51, o));
		tree.removeMin();
		tree.removeMax();
		tree.add(51, o);
		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsInstance(51, o));
		tree.removeInstance(51, o);
		Assert.assertEquals(0, tree.size());
		Assert.assertFalse(tree.containsInstance(51, o));
		
		for (int i = 0; i < 100; ++i)
			tree.add(i, o);
		Assert.assertEquals(100, tree.size());
		Assert.assertTrue(tree.contains(99, o));
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
