package net.lecousin.framework.core.tests.memory;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.ObjectBank;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;

import org.junit.Assert;
import org.junit.Test;

public class TestObjectBank extends LCCoreAbstractTest {

	@Test
	public void test() {
		ObjectBank<Integer> bank = new ObjectBank<>(3, "test");
		Assert.assertNull(bank.get());
		bank.free(Integer.valueOf(1));
		Assert.assertEquals(1, bank.get().intValue());
		Assert.assertNull(bank.get());
		bank.free(Integer.valueOf(2));
		bank.free(Integer.valueOf(3));
		bank.free(Integer.valueOf(4));
		bank.free(Integer.valueOf(5));
		Integer i;
		i = bank.get();
		Assert.assertTrue(i != null && i.intValue() >= 2 && i.intValue() <= 4);
		i = bank.get();
		Assert.assertTrue(i != null && i.intValue() >= 2 && i.intValue() <= 4);
		i = bank.get();
		Assert.assertTrue(i != null && i.intValue() >= 2 && i.intValue() <= 4);
		Assert.assertNull(bank.get());
		
		bank.getDescription();
		bank.getItemsDescription();
		bank.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		bank.freeMemory(FreeMemoryLevel.LOW);
		bank.freeMemory(FreeMemoryLevel.MEDIUM);
		bank.freeMemory(FreeMemoryLevel.URGENT);
		
		bank.close();
	}
	
}
