package net.lecousin.framework.core.tests;

import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.mutable.MutableBoolean;

public class TestRunListener extends RunListener {

	@Override
	public void testRunFinished(Result result) {
		if (result.wasSuccessful()) {
			MutableBoolean stopped = LCCore.stop(false);
			synchronized (stopped) {
				try {
					while (!stopped.get())
						stopped.wait();
				} catch (InterruptedException e) {
					// stop
				}
			}
		}
	}
	
}
