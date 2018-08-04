package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.sort.RedBlackTreeInteger;
import net.lecousin.framework.collections.sort.RedBlackTreeInteger.Node;
import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.collections.sort.Sorted.AssociatedWithInteger;
import net.lecousin.framework.core.test.collections.sort.TestSortedAssociatedWithInteger;

public class TestRedBlackTreeInteger extends TestSortedAssociatedWithInteger {

	@Override
	protected AssociatedWithInteger<Object> createSorted() {
		return new RedBlackTreeInteger<Object>();
	}
	
	@Override
	protected void check(Sorted.AssociatedWithInteger<Object> sorted, TreeSet<Integer> order) {
		super.check(sorted, order);
		RedBlackTreeInteger<Object> tree = (RedBlackTreeInteger<Object>)sorted;
		if (!tree.isEmpty()) {
			Node<Object> n = tree.getMin();
			Assert.assertFalse(n == null);
			Iterator<Integer> it = order.iterator();
			Assert.assertEquals(it.next().intValue(), n.getValue());
			Node<Object> n2 = n;
			while (it.hasNext()) {
				int val = n2.getValue();
				n2 = tree.getNext(n2);
				Assert.assertFalse(n2 == null);
				Assert.assertEquals(it.next().intValue(), n2.getValue());
				Assert.assertTrue(n2 == tree.getNext(val));
			}
			n2 = tree.getNext(n2);
			Assert.assertTrue(n2 == null);
			n2 = tree.getNext(tree.getMax().getValue());
			Assert.assertTrue(n2 == null);

			n = tree.getMax();
			Assert.assertFalse(n == null);
			it = order.descendingIterator();
			Assert.assertEquals(it.next().intValue(), n.getValue());
			n2 = n;
			while (it.hasNext()) {
				int val = n2.getValue();
				n2 = tree.getPrevious(n2);
				Assert.assertFalse(n2 == null);
				Assert.assertEquals(it.next().intValue(), n2.getValue());
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
		RedBlackTreeInteger<Object> tree = new RedBlackTreeInteger<Object>();
		Integer o = Integer.valueOf(1664);
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
		
		tree = new RedBlackTreeInteger<Object>();
		Assert.assertNull(tree.searchNearestLower(22));
		for (int i = 0; i < 100; i += 10)
			tree.add(i, Integer.valueOf(-i));
		Assert.assertEquals(0, tree.searchNearestLower(1).getValue());
		Assert.assertEquals(0, tree.searchNearestLower(10).getValue());
		Assert.assertEquals(10, tree.searchNearestLower(11).getValue());
		Assert.assertEquals(10, tree.searchNearestLower(20).getValue());
		Assert.assertEquals(20, tree.searchNearestLower(22).getValue());
		Assert.assertEquals(20, tree.searchNearestLower(30).getValue());
		Assert.assertEquals(30, tree.searchNearestLower(33).getValue());
		Assert.assertEquals(30, tree.searchNearestLower(40).getValue());
		Assert.assertEquals(40, tree.searchNearestLower(44).getValue());
		Assert.assertEquals(40, tree.searchNearestLower(50).getValue());
		Assert.assertEquals(50, tree.searchNearestLower(55).getValue());
		Assert.assertEquals(50, tree.searchNearestLower(60).getValue());
		Assert.assertEquals(60, tree.searchNearestLower(66).getValue());
		Assert.assertEquals(60, tree.searchNearestLower(70).getValue());
		Assert.assertEquals(70, tree.searchNearestLower(77).getValue());
		Assert.assertEquals(70, tree.searchNearestLower(80).getValue());
		Assert.assertEquals(80, tree.searchNearestLower(88).getValue());
		Assert.assertEquals(80, tree.searchNearestLower(90).getValue());
		Assert.assertEquals(90, tree.searchNearestLower(99).getValue());
	}
	
}
