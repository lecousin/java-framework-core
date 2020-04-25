package net.lecousin.framework.core.tests.log;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger;

import org.junit.Assert;
import org.junit.Test;

public class TestLogPattern extends LCCoreAbstractTest {

	@Test
	public void tests() {
		Log log = new Log();
		log.level = Logger.Level.INFO;
		log.location = new Exception().getStackTrace()[0];
		log.loggerName = "test_logger";
		log.message = "this is the message";
		log.threadName = "theThread";
		log.timestamp = System.currentTimeMillis();
		log.app = LCCore.getApplication();
		
		expect("no pattern", log, "no pattern");
		// TODO continue
		
	}
	
	private static void expect(String pattern, Log log, String expected) {
		StringBuilder s = new LogPattern(pattern).generate(log);
		Assert.assertEquals(expected, s.toString());
	}
	
}
