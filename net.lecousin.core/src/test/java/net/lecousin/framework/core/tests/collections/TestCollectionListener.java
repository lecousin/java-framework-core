package net.lecousin.framework.core.tests.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.CollectionListener;
import net.lecousin.framework.collections.CollectionListener.Keep;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.Mutable;

public class TestCollectionListener extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void testKeep() {
		CollectionListener.Keep<Integer> keep = new Keep<>(new ArrayList<>(), Task.PRIORITY_NORMAL);
		ArrayList<Integer> col = new ArrayList<>();
		ArrayList<Integer> changed = new ArrayList<>();
		Mutable<SynchronizationPoint<Exception>> s = new Mutable<>(new SynchronizationPoint<>());
		CollectionListener<Integer> listener;
		keep.addListener(listener = new CollectionListener<Integer>() {
			@Override
			public void elementsAdded(Collection<? extends Integer> elements) {
				col.addAll(elements);
				s.get().unblock();
			}

			@Override
			public void elementsRemoved(Collection<? extends Integer> elements) {
				col.removeAll(elements);
				s.get().unblock();
			}

			@Override
			public void elementsChanged(Collection<? extends Integer> elements) {
				changed.addAll(elements);
				s.get().unblock();
			}

			@Override
			public void error(Throwable error) {
				s.get().error(new Exception(error));
			}
		});
		
		s.get().block(0);
		Assert.assertTrue(col.isEmpty());
		
		s.set(new SynchronizationPoint<>());
		keep.elementsReady(Arrays.asList(Integer.valueOf(10), Integer.valueOf(20)));
		s.get().block(0);
		Assert.assertEquals(2, col.size());
		Assert.assertTrue(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		
		s.set(new SynchronizationPoint<>());
		keep.elementsRemoved(Arrays.asList(Integer.valueOf(10)));
		s.get().block(0);
		Assert.assertEquals(1, col.size());
		Assert.assertFalse(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		
		s.set(new SynchronizationPoint<>());
		keep.elementsAdded(Arrays.asList(Integer.valueOf(30), Integer.valueOf(40)));
		s.get().block(0);
		Assert.assertEquals(3, col.size());
		Assert.assertFalse(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		Assert.assertTrue(col.contains(Integer.valueOf(30)));
		Assert.assertTrue(col.contains(Integer.valueOf(40)));

		s.set(new SynchronizationPoint<>());
		keep.elementsRemoved(Arrays.asList(Integer.valueOf(30)));
		s.get().block(0);
		Assert.assertEquals(2, col.size());
		Assert.assertFalse(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		Assert.assertFalse(col.contains(Integer.valueOf(30)));
		Assert.assertTrue(col.contains(Integer.valueOf(40)));
		
		s.set(new SynchronizationPoint<>());
		changed.clear();
		keep.elementsChanged(Arrays.asList(Integer.valueOf(20), Integer.valueOf(40)));
		s.get().block(0);
		Assert.assertEquals(2, col.size());
		Assert.assertFalse(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		Assert.assertFalse(col.contains(Integer.valueOf(30)));
		Assert.assertTrue(col.contains(Integer.valueOf(40)));
		Assert.assertTrue(changed.contains(Integer.valueOf(20)));
		Assert.assertTrue(changed.contains(Integer.valueOf(40)));
		
		keep.removeListener(listener);
		s.set(new SynchronizationPoint<>());
		keep.elementsReady(Arrays.asList(Integer.valueOf(100), Integer.valueOf(200)));
		s.get().block(3000);
		Assert.assertFalse(s.get().isUnblocked());
		Assert.assertEquals(2, col.size());
		Assert.assertFalse(col.contains(Integer.valueOf(10)));
		Assert.assertTrue(col.contains(Integer.valueOf(20)));
		Assert.assertFalse(col.contains(Integer.valueOf(30)));
		Assert.assertTrue(col.contains(Integer.valueOf(40)));

		s.set(new SynchronizationPoint<>());
		keep.addListener(listener);
		s.get().block(0);
		s.set(new SynchronizationPoint<>());
		keep.error(new Exception());
		s.get().block(0);
		Assert.assertNotNull(s.get().getError());
		
		Mutable<SynchronizationPoint<Exception>> s2 = new Mutable<>(new SynchronizationPoint<>());
		CollectionListener<Integer> listener2;
		keep.addListener(listener2 = new CollectionListener<Integer>() {
			@Override
			public void elementsAdded(Collection<? extends Integer> elements) {
			}

			@Override
			public void elementsRemoved(Collection<? extends Integer> elements) {
			}

			@Override
			public void elementsChanged(Collection<? extends Integer> elements) {
			}

			@Override
			public void error(Throwable error) {
				s2.get().error(new Exception(error));
			}
		});
		s2.get().block(0);
		Assert.assertNotNull(s2.get().getError());
		
		keep.removeListener(listener2);
		keep.removeListener(listener);
	}
	
}
