package net.lecousin.framework.core.tests.application;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.StandaloneLCCore;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestLCCore extends LCCoreAbstractTest {

	@Test
	public void test() {
		try {
			LCCore.get().add(LCCore.getApplication());
			throw new AssertionError();
		} catch (IllegalStateException e) {
			// ok
		}
		try {
			LCCore.get().add(LCCore.getApplication());
			throw new AssertionError();
		} catch (IllegalStateException e) {
			// ok
		}
		try {
			((StandaloneLCCore)LCCore.get()).setCPUThreads(1);
			throw new AssertionError();
		} catch (IllegalStateException e) {
			// ok
		}
		try {
			((StandaloneLCCore)LCCore.get()).setUnmanagedThreads(1);
			throw new AssertionError();
		} catch (IllegalStateException e) {
			// ok
		}
		try {
			((StandaloneLCCore)LCCore.get()).setDrivesProvider(null);
			throw new AssertionError();
		} catch (IllegalStateException e) {
			// ok
		}
	}
	
}
