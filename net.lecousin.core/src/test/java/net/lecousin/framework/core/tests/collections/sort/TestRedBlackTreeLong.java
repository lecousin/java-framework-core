package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.TreeSet;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.RedBlackTreeLong.Node;
import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.core.test.collections.sort.TestSortedAssociatedWithLong;

import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertNull(tree.getPrevious(51));
		Assert.assertNull(tree.getPrevious(50));
		Assert.assertNull(tree.getPrevious(52));
		Assert.assertNull(tree.getPrevious(tree.get(51)));
		Assert.assertNull(tree.getNext(51));
		Assert.assertNull(tree.getNext(50));
		Assert.assertNull(tree.getNext(52));
		Assert.assertNull(tree.getNext(tree.get(51)));
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
		
		tree.add(51, o);
		tree.add(52, o);
		Assert.assertEquals(2, tree.size());
		Assert.assertTrue(tree.containsInstance(51, o));
		Assert.assertTrue(tree.containsInstance(52, o));
		Assert.assertNull(tree.getPrevious(51));
		Assert.assertNotNull(tree.getPrevious(52));
		Assert.assertEquals(51, tree.getPrevious(52).getValue());
		Assert.assertNotNull(tree.getNext(51));
		Assert.assertEquals(52, tree.getNext(51).getValue());
		Assert.assertNull(tree.getNext(52));
		tree.add(53, o);
		tree.removeKey(51);
		Assert.assertNull(tree.getPrevious(52));
		Assert.assertEquals(53, tree.getNext(52).getValue());
		Assert.assertNull(tree.getNext(53));

		tree.clear();
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
		
		tree = new RedBlackTreeLong<Object>();
		Assert.assertNull(tree.searchNearestLower(22, false));
		for (int i = 0; i < 100; i += 10)
			tree.add(i, Integer.valueOf(-i));
		Assert.assertEquals(0, tree.searchNearestLower(1, false).getValue());
		Assert.assertEquals(0, tree.searchNearestLower(10, false).getValue());
		Assert.assertEquals(10, tree.searchNearestLower(10, true).getValue());
		Assert.assertEquals(10, tree.searchNearestLower(11, false).getValue());
		Assert.assertEquals(10, tree.searchNearestLower(20, false).getValue());
		Assert.assertEquals(20, tree.searchNearestLower(20, true).getValue());
		Assert.assertEquals(20, tree.searchNearestLower(22, false).getValue());
		Assert.assertEquals(20, tree.searchNearestLower(30, false).getValue());
		Assert.assertEquals(30, tree.searchNearestLower(30, true).getValue());
		Assert.assertEquals(30, tree.searchNearestLower(33, false).getValue());
		Assert.assertEquals(30, tree.searchNearestLower(40, false).getValue());
		Assert.assertEquals(40, tree.searchNearestLower(40, true).getValue());
		Assert.assertEquals(40, tree.searchNearestLower(44, false).getValue());
		Assert.assertEquals(40, tree.searchNearestLower(50, false).getValue());
		Assert.assertEquals(50, tree.searchNearestLower(50, true).getValue());
		Assert.assertEquals(50, tree.searchNearestLower(55, false).getValue());
		Assert.assertEquals(50, tree.searchNearestLower(60, false).getValue());
		Assert.assertEquals(60, tree.searchNearestLower(60, true).getValue());
		Assert.assertEquals(60, tree.searchNearestLower(66, false).getValue());
		Assert.assertEquals(60, tree.searchNearestLower(70, false).getValue());
		Assert.assertEquals(70, tree.searchNearestLower(70, true).getValue());
		Assert.assertEquals(70, tree.searchNearestLower(77, false).getValue());
		Assert.assertEquals(70, tree.searchNearestLower(80, false).getValue());
		Assert.assertEquals(80, tree.searchNearestLower(80, true).getValue());
		Assert.assertEquals(80, tree.searchNearestLower(88, false).getValue());
		Assert.assertEquals(80, tree.searchNearestLower(90, false).getValue());
		Assert.assertEquals(90, tree.searchNearestLower(90, true).getValue());
		Assert.assertEquals(90, tree.searchNearestLower(99, false).getValue());

		Assert.assertEquals(10, tree.searchNearestHigher(0, false).getValue());
		Assert.assertEquals(10, tree.searchNearestHigher(9, false).getValue());
		Assert.assertEquals(20, tree.searchNearestHigher(10, false).getValue());
		Assert.assertEquals(10, tree.searchNearestHigher(10, true).getValue());
		Assert.assertEquals(20, tree.searchNearestHigher(11, false).getValue());
		Assert.assertNull(tree.searchNearestHigher(99, false));
	}

	
	@Test
	public void testAddAndRemoveWithCommonValues() {
		RedBlackTreeLong<Object> tree = new RedBlackTreeLong<Object>();
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		tree.add(10, o1);
		tree.add(10, o2);
		tree.add(10, o3);
		tree.add(10, o4);
		Assert.assertEquals(4, tree.size());
		
		tree.remove(getNode(tree, o2));
		Assert.assertEquals(3, tree.size());
		Assert.assertTrue(tree.contains(10, o1));
		Assert.assertFalse(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertTrue(tree.contains(10, o4));

		tree.remove(getNode(tree, o4));
		Assert.assertEquals(2, tree.size());
		Assert.assertTrue(tree.contains(10, o1));
		Assert.assertFalse(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));
		
		tree.add(10, o2);
		Assert.assertEquals(3, tree.size());
		Assert.assertTrue(tree.contains(10, o1));
		Assert.assertTrue(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));
		
		tree.remove(getNode(tree, o1));
		Assert.assertEquals(2, tree.size());
		Assert.assertFalse(tree.contains(10, o1));
		Assert.assertTrue(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));

		tree.add(10, o1);
		Assert.assertEquals(3, tree.size());
		Assert.assertTrue(tree.contains(10, o1));
		Assert.assertTrue(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));

		tree.remove(getNode(tree, o2));
		Assert.assertEquals(2, tree.size());
		Assert.assertTrue(tree.contains(10, o1));
		Assert.assertFalse(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));

		tree.remove(getNode(tree, o1));
		Assert.assertEquals(1, tree.size());
		Assert.assertFalse(tree.contains(10, o1));
		Assert.assertFalse(tree.contains(10, o2));
		Assert.assertTrue(tree.contains(10, o3));
		Assert.assertFalse(tree.contains(10, o4));
		
		tree.remove(getNode(tree, o3));
		Assert.assertEquals(0, tree.size());
	}

	private static RedBlackTreeLong.Node<Object> getNode(RedBlackTreeLong<Object> tree, Object o) {
		for (Iterator<RedBlackTreeLong.Node<Object>> it = tree.nodeIterator(); it.hasNext(); ) {
			RedBlackTreeLong.Node<Object> node = it.next();
			if (node.getElement().equals(o))
				return node;
		}
		throw new AssertionError("Node not found");
	}

}
