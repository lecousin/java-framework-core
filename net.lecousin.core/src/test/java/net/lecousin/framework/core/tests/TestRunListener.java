package net.lecousin.framework.core.tests;

import net.lecousin.framework.application.LCCore;

import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class TestRunListener extends RunListener {

	@Override
	public void testRunFinished(Result result) throws Exception {
		if (result.wasSuccessful())
			LCCore.stop(false);
	}
	
}
