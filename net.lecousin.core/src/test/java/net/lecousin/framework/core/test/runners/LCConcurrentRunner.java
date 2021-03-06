package net.lecousin.framework.core.test.runners;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class LCConcurrentRunner extends BlockJUnit4ClassRunner {

	public LCConcurrentRunner(Class<?> klass) throws InitializationError {
		super(klass);
		setScheduler(createRunnerScheduler());
	}
	
	public static class Parameterized extends org.junit.runners.Parameterized {

		public Parameterized(Class<?> klass) throws Throwable {
			super(klass);
			setScheduler(createRunnerScheduler());
		}
		
	}
	
	public static class ConcurrentParameterized extends BlockJUnit4ClassRunnerWithParameters {

		public ConcurrentParameterized(TestWithParameters test) throws InitializationError {
			super(test);
			setScheduler(createRunnerScheduler());
		}
		
	}
	
	public static class ConcurrentParameterizedRunnedFactory implements ParametersRunnerFactory {
		@Override
		public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
			return new ConcurrentParameterized(test);
		}
	}
	
	public static RunnerScheduler createRunnerScheduler() throws InitializationError {
		try {
			LCCoreAbstractTest.init();
		} catch (Exception e) {
			throw new InitializationError(e);
		}
		return new RunnerScheduler() {
			
			private JoinPoint<NoException> jp = new JoinPoint<>();
			
			@Override
	        public void schedule(Runnable childStatement) {
				jp.addToJoin(Task.cpu("Execute JUnit test", Task.Priority.LOW, new Executable.FromRunnable(childStatement)).setMaxBlockingTimeInNanoBeforeToLog(Long.MAX_VALUE).start());
	        }
	
			@Override
	        public void finished() {
				jp.start();
				int lastNb = jp.getToJoin();
				do {
					jp.block(10L * 60 * 1000);
					if (lastNb == jp.getToJoin())
						break;
					lastNb = jp.getToJoin();
				} while (!jp.isDone());
				if (!jp.isDone())
					throw new RuntimeException("Some tests are not finished after 10 minutes: still waiting for " + jp.getToJoin());
	        }
	    };
	};

}
