package net.lecousin.framework.core.test;

import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class TestRunListener extends RunListener {

	@Override
	public void testRunFinished(Result result) {
		if (result.wasSuccessful()) {
			LCTest.close();
		}
	}
	
}
