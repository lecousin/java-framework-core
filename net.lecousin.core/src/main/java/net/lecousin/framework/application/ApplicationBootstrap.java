package net.lecousin.framework.application;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.progress.FakeWorkProgress;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Interface to start an application.
 */
public interface ApplicationBootstrap {

	/**
	 * Start the application, and return a synchronization point that will be unblocked when the application
	 * is asked to shutdown. The method {@link WorkProgress#done()} must be called on the given progress
	 * to signal the end of the startup.
	 */
	public ISynchronizationPoint<Exception> start(Application app, WorkProgress progress) throws Exception;
	
	/**
	 * Utility method to start an application in a main.
	 */
	public static void main(Artifact artifact, String[] args, boolean debugMode, ApplicationBootstrap startup) {
		ISynchronizationPoint<Exception> start = Application.start(artifact, args, debugMode);
		RunInMain t = new RunInMain(startup);
		t.startOn(start, false);
		start.block(0);
		t.getOutput().block(0);
		t.getResult().block(0);
		LCCore.stop(true);
	}
	
	/**
	 * Utility class that runs the bootstrap in a task.
	 */
	public static class RunInMain extends Task.Cpu<ISynchronizationPoint<Exception>, Exception> {
		/** Constructor. */
		public RunInMain(ApplicationBootstrap bootstrap) {
			super("Start application", Task.PRIORITY_NORMAL);
			this.bootstrap = bootstrap;
		}
		
		protected ApplicationBootstrap bootstrap;
		
		@Override
		public ISynchronizationPoint<Exception> run() throws Exception {
			return bootstrap.start(LCCore.getApplication(), new FakeWorkProgress());
		}
	}
	
}
