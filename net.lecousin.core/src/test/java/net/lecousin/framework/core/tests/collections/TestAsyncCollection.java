package net.lecousin.framework.core.tests.collections;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.AsyncCollection;
import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableBoolean;

public class TestAsyncCollection extends LCCoreAbstractTest {

	@Test
	public void testListen() {
		List<Integer> list = new LinkedList<>();
		MutableBoolean done = new MutableBoolean(false);
		AsyncCollection.Listen<Integer> col = new AsyncCollection.Listen<>(
			(elements) -> {
				list.addAll(elements);
			},
			() -> {
				done.set(true);
			},
			(error) -> {
				// TODO
			}
		);
		
		Assert.assertTrue(list.isEmpty());
		Assert.assertFalse(done.get());
		col.newElements(Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111)));
		Assert.assertTrue(CollectionsUtil.equals(list, Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111))));
		Assert.assertFalse(done.get());
		col.newElements(Arrays.asList(Integer.valueOf(2), Integer.valueOf(22)));
		Assert.assertTrue(CollectionsUtil.equals(list, Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertFalse(done.get());
		Assert.assertFalse(col.isDone());
		col.done();
		Assert.assertTrue(CollectionsUtil.equals(list, Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertTrue(done.get());
		Assert.assertTrue(col.isDone());
	}
	
	@Test
	public void testKeep() {
		MutableBoolean done = new MutableBoolean(false);
		AsyncCollection.Keep<Integer> col = new AsyncCollection.Keep<>();
		col.ondone(() -> { done.set(true); });
		
		Assert.assertTrue(col.getCurrentElements().isEmpty());
		Assert.assertFalse(done.get());
		col.newElements(Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111)));
		Assert.assertTrue(CollectionsUtil.equals(col.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111))));
		Assert.assertFalse(done.get());
		col.newElements(Arrays.asList(Integer.valueOf(2), Integer.valueOf(22)));
		Assert.assertTrue(CollectionsUtil.equals(col.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertFalse(done.get());
		Assert.assertFalse(col.isDone());
		col.done();
		Assert.assertTrue(CollectionsUtil.equals(col.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertTrue(done.get());
		Assert.assertTrue(col.isDone());
	}
	
	@Test
	public void testAggregator() {
		AsyncCollection.Keep<Integer> main = new AsyncCollection.Keep<>();
		AsyncCollection.Aggregator<Integer> aggr = new AsyncCollection.Aggregator<>(3, main);
		MutableBoolean done = new MutableBoolean(false);
		main.ondone(() -> { done.set(true); });
		
		Assert.assertTrue(main.getCurrentElements().isEmpty());
		Assert.assertFalse(done.get());
		aggr.newElements(Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111)));
		Assert.assertTrue(CollectionsUtil.equals(main.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111))));
		Assert.assertFalse(done.get());
		Assert.assertFalse(main.isDone());
		aggr.done();
		Assert.assertFalse(done.get());
		Assert.assertFalse(main.isDone());
		aggr.newElements(Arrays.asList(Integer.valueOf(2), Integer.valueOf(22)));
		Assert.assertTrue(CollectionsUtil.equals(main.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertFalse(done.get());
		Assert.assertFalse(main.isDone());
		aggr.done();
		Assert.assertFalse(done.get());
		Assert.assertFalse(main.isDone());
		aggr.newElements(Arrays.asList(Integer.valueOf(3)));
		Assert.assertTrue(CollectionsUtil.equals(main.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22), Integer.valueOf(3))));
		Assert.assertFalse(done.get());
		Assert.assertFalse(main.isDone());
		Assert.assertFalse(aggr.isDone());
		aggr.done();
		Assert.assertTrue(done.get());
		Assert.assertTrue(main.isDone());
		Assert.assertTrue(aggr.isDone());
		Assert.assertTrue(CollectionsUtil.equals(main.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22), Integer.valueOf(3))));
	}
	
}
