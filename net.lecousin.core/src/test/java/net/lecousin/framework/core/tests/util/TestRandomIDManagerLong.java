package net.lecousin.framework.core.tests.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.RandomIDManagerLong;

public class TestRandomIDManagerLong extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() {
		RandomIDManagerLong idm = new RandomIDManagerLong();
		Set<Long> used = new HashSet<Long>();
		for (int i = 0; i < 1000; ++i) {
			long id = idm.allocate();
			Assert.assertTrue(used.add(Long.valueOf(id)));
		}
		for (long id = 159; id < 587; ++id) {
			Long l = Long.valueOf(id);
			if (!used.contains(l)) {
				used.add(l);
				idm.used(id);
			}
		}
		for (long id = 4590; id < 5870; ++id) {
			Long l = Long.valueOf(id);
			if (!used.contains(l)) {
				used.add(l);
				idm.used(id);
			}
		}
		for (Iterator<Long> it = used.iterator(); it.hasNext(); ) {
			idm.free(it.next().longValue());
		}
	}
	
}
