package net.lecousin.framework.core.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import net.lecousin.framework.LCCoreVersion;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.StandaloneLCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.memory.MemoryManager;

public abstract class LCCoreAbstractTest {

	@BeforeClass
	public static synchronized void init() throws Exception {
		if (LCCore.get() != null) return;
		Application.start(new Artifact("net.lecousin.framework.test", "test", new Version(LCCoreVersion.VERSION)), true).blockThrow(0);
		LCCore.getApplication().getLoggerFactory().getLogger(SynchronizationPoint.class).setLevel(Logger.Level.INFO);
		StandaloneLCCore.setLogThreadingInterval(120000);
		MemoryManager.logMemory(120000, Level.DEBUG);
	}
	
	@AfterClass
	public static synchronized void close() {
		//Application.instance.stop(false);
	}
	
	public static void assertException(Runnable toRun, Class<? extends Exception> expectedError) {
		try {
			toRun.run();
		} catch (Throwable t) {
			if (!expectedError.equals(t.getClass()))
				throw new AssertionError("Expected exception is " + expectedError.getName() + ", raised is " + t.getClass().getName());
			return;
		}
		throw new AssertionError("No exception raised, expected is " + expectedError.getName());
	}
	
}
