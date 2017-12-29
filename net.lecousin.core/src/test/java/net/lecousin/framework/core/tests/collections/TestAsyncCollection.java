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
		col.done();
		Assert.assertTrue(CollectionsUtil.equals(list, Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertTrue(done.get());
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
		col.done();
		Assert.assertTrue(CollectionsUtil.equals(col.getCurrentElements(), Arrays.asList(Integer.valueOf(1), Integer.valueOf(11), Integer.valueOf(111), Integer.valueOf(2), Integer.valueOf(22))));
		Assert.assertTrue(done.get());
	}
	
}
