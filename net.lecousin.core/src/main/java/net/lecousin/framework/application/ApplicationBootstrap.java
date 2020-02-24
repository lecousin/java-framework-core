package net.lecousin.framework.application;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
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
	IAsync<Exception> start(Application app, WorkProgress progress) throws ApplicationBootstrapException;
	
	/**
	 * Utility method to start an application in a main.
	 */
	static void main(Artifact artifact, String[] args, boolean debugMode, ApplicationBootstrap startup) {
		IAsync<ApplicationBootstrapException> start = Application.start(artifact, args, debugMode);
		Task<IAsync<Exception>, Exception> t = Task.cpu("Start application",
			() -> startup.start(LCCore.getApplication(), new FakeWorkProgress()));
		t.startOn(start, false);
		start.block(0);
		t.getOutput().block(0);
		if (t.getOutput().isSuccessful())
			t.getOutput().getResult().block(0);
		LCCore.stop(true);
	}
	
}
