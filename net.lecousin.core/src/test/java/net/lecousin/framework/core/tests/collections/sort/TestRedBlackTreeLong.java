package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Assert;

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
		} else {
			Node<Object> n = tree.getMin();
			Assert.assertTrue(n == null);
			n = tree.getMax();
			Assert.assertTrue(n == null);
		}
	}
	
}
