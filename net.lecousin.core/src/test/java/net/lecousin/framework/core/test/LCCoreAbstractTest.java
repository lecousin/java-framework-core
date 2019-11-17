package net.lecousin.framework.core.test;

import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.LCCoreVersion;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.StandaloneLCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.memory.MemoryManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(LCConcurrentRunner.class)
public abstract class LCCoreAbstractTest {

	@BeforeClass
	public static synchronized void init() throws Exception {
		if (LCCore.get() != null) return;
		Application.start(new Artifact("net.lecousin.framework.test", "test", new Version(LCCoreVersion.VERSION)), true).blockThrow(0);
		LCCore.getApplication().getLoggerFactory().getLogger(Async.class).setLevel(Logger.Level.INFO);
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
	
	public static Collection<Object[]> addTestParameter(Collection<Object[]> params, Object... additionalParameterCases) {
		ArrayList<Object[]> list = new ArrayList<>(params.size() * additionalParameterCases.length);
		for (Object[] prevParams : params) {
			for (Object newParam : additionalParameterCases) {
				Object[] newParams = new Object[prevParams.length + 1];
				System.arraycopy(prevParams, 0, newParams, 0, prevParams.length);
				newParams[prevParams.length] = newParam;
				list.add(newParams);
			}
		}
		return list;
	}
	
}
