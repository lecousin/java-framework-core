package net.lecousin.framework.core.test.runners;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class LCSequentialRunner extends BlockJUnit4ClassRunner {

	public LCSequentialRunner(Class<?> klass) throws InitializationError {
		super(klass);
		setScheduler(createRunnerScheduler());
	}
	
	public static class Parameterized extends org.junit.runners.Parameterized {

		public Parameterized(Class<?> klass) throws Throwable {
			super(klass);
			setScheduler(createRunnerScheduler());
		}
		
	}
	
	public static class SequentialParameterized extends BlockJUnit4ClassRunnerWithParameters {

		public SequentialParameterized(TestWithParameters test) throws InitializationError {
			super(test);
			setScheduler(createRunnerScheduler());
		}
		
	}
	
	public static class SequentialParameterizedRunnedFactory implements ParametersRunnerFactory {
		@Override
		public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
			return new SequentialParameterized(test);
		}
	}
	
	public static RunnerScheduler createRunnerScheduler() throws InitializationError {
		try {
			LCCoreAbstractTest.init();
		} catch (Exception e) {
			throw new InitializationError(e);
		}
		return new RunnerScheduler() {
			
			@Override
	        public void schedule(Runnable childStatement) {
				Task.cpu("Execute JUnit test", Task.Priority.LOW, new Executable.FromRunnable(childStatement))
				.setMaxBlockingTimeInNanoBeforeToLog(Long.MAX_VALUE)
				.resetContext()
				.start()
				.getOutput().block(0);
	        }
	
			@Override
	        public void finished() {
	        }
	    };
	};

}
