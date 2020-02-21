package net.lecousin.framework.core.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;

import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(LCConcurrentRunner.class)
public abstract class LCCoreAbstractTest {

	private static List<Object> toCloseBeforeTest;
	
	@BeforeClass
	public static synchronized void init() throws Exception {
		if (LCCore.get() != null) return;
		LCTest.init();
	}
	
	@BeforeClass
	public static void saveResourcesToClose() {
		toCloseBeforeTest = LCCore.getApplication().getResourcesToClose();
	}
	
	@AfterClass
	public static void checkResourcesToClose() {
		List<Object> remaining = LCCore.getApplication().getResourcesToClose();
		for (Iterator<Object> it = remaining.iterator(); it.hasNext(); ) {
			if (toCloseBeforeTest.remove(it.next()))
				it.remove();
		}
		if (remaining.isEmpty())
			return;
		StringBuilder s = new StringBuilder();
		s.append("Test class didn't close the following ").append(remaining.size()).append(" resource(s):\r\n");
		for (Object o : remaining)
			s.append(" - ").append(o).append("\r\n");
		System.err.println(s.toString());
	}
	
	@Rule
	public final TestStatusToConsole statusToConsole = new TestStatusToConsole();
	
	public static class TestStatusToConsole extends TestWatcher {
		
		public Description currentTest;
		
		@Override
		protected void starting(Description description) {
			currentTest = description;
			System.out.println("[Test-Start]     " + description);
		}
		
		@Override
		protected void succeeded(Description description) {
			System.out.println("[Test-Succeeded] " + description);
		}

		@Override
		protected void failed(Throwable e, Description description) {
			System.out.println("[Test-Failed]    " + description);
		}

		@Override
		protected void skipped(AssumptionViolatedException e, Description description) {
			System.out.println("[Test-Skipped]   " + description);
		}

		@Override
		protected void finished(Description description) {
			currentTest = null;
		}
	};
	
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
