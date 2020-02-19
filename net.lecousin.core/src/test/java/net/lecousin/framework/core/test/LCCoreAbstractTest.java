package net.lecousin.framework.core.test;

import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(LCConcurrentRunner.class)
public abstract class LCCoreAbstractTest {

	@BeforeClass
	public static synchronized void init() throws Exception {
		if (LCCore.get() != null) return;
		LCTest.init();
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
