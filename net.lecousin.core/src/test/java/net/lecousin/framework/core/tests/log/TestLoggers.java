package net.lecousin.framework.core.tests.log;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectoryContentTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.tests.log.Loggers.Logger1;
import net.lecousin.framework.core.tests.log.Loggers.Logger2;
import net.lecousin.framework.core.tests.log.Loggers.Logger3;
import net.lecousin.framework.core.tests.log.Loggers.Logger4;
import net.lecousin.framework.core.tests.log.Loggers.Logger5;
import net.lecousin.framework.core.tests.log.Loggers.LoggerForPattern;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.log.appenders.RollingFileAppender;
import net.lecousin.framework.text.StringUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestLoggers extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		Application app = LCCore.getApplication();
		File dir = new File(app.getProperty(Application.PROPERTY_LOG_DIRECTORY));
		// clean
		new RemoveDirectoryContentTask(dir, null, 0, Task.PRIORITY_NORMAL, false).start().getOutput().blockThrow(0);
		// configure
		LoggerFactory factory = app.getLoggerFactory();
		factory.configure("classpath:log/test-logging.xml");
		// get loggers
		Logger log1 = factory.getLogger(Logger1.class);
		Logger log2 = factory.getLogger(Logger2.class);
		Logger log3 = factory.getLogger(Logger3.class);
		Logger log4 = factory.getLogger(Logger4.class);
		Logger log5 = factory.getLogger(Logger5.class);
		// produce some logs
		produceLogs(log1);
		produceLogs(log2);
		produceLogs(log3);
		produceLogs(log4);
		produceLogs(log5);
		// flush
		factory.flush().blockThrow(0);
		// check
		String log = IOUtil.readFullyAsStringSync(new File(dir, "log1.txt"), StandardCharsets.UTF_8);
		Assert.assertEquals(1, StringUtil.count(log, "this is fatal"));
		Assert.assertEquals(1, StringUtil.count(log, "this is error"));
		Assert.assertEquals(1, StringUtil.count(log, "this is warn"));
		Assert.assertEquals(1, StringUtil.count(log, "this is info"));
		Assert.assertEquals(1, StringUtil.count(log, "this is debug"));
		Assert.assertEquals(0, StringUtil.count(log, "this is trace"));

		log = IOUtil.readFullyAsStringSync(new File(dir, "log2.txt"), StandardCharsets.UTF_8);
		Assert.assertEquals(4, StringUtil.count(log, "this is fatal"));
		Assert.assertEquals(4, StringUtil.count(log, "this is error"));
		Assert.assertEquals(4, StringUtil.count(log, "this is warn"));
		Assert.assertEquals(2, StringUtil.count(log, "this is info"));
		Assert.assertEquals(1, StringUtil.count(log, "this is debug"));
		Assert.assertEquals(1, StringUtil.count(log, "this is trace"));

		log = IOUtil.readFullyAsStringSync(new File(dir, "log3.txt"), StandardCharsets.UTF_8);
		Assert.assertEquals(3, StringUtil.count(log, "this is fatal"));
		Assert.assertEquals(3, StringUtil.count(log, "this is error"));
		Assert.assertEquals(3, StringUtil.count(log, "this is warn"));
		Assert.assertEquals(2, StringUtil.count(log, "this is info"));
		Assert.assertEquals(2, StringUtil.count(log, "this is debug"));
		Assert.assertEquals(1, StringUtil.count(log, "this is trace"));
		
		factory.getLogger(LoggerForPattern.class).debug("test");
		factory.flush().blockThrow(0);
		log = IOUtil.readFullyAsStringSync(new File(dir, "logPattern.txt"), StandardCharsets.UTF_8);
		int i = log.indexOf("@@ ");
		Assert.assertTrue(i >= 0);
		log = log.substring(i + 3);
		i = log.indexOf(" @@");
		Assert.assertTrue(i >= 0);
		log = log.substring(0, i);
		StringBuilder expected = new StringBuilder();
		Calendar cal = Calendar.getInstance();
		expected.append(cal.get(Calendar.YEAR)).append('-');
		StringUtil.paddingLeft(expected, Integer.toString(cal.get(Calendar.MONTH) + 1), 2, '0');
		expected.append('-');
		StringUtil.paddingLeft(expected, Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0');
		expected.append(' ');
		expected.append(Thread.currentThread().getName());
		expected.append(' ');
		expected.append("DEBUG");
		expected.append(' ');
		expected.append(LoggerForPattern.class.getName());
		expected.append(" hello % ");
		expected.append(TestLoggers.class.getName());
		expected.append(' ');
		expected.append("test");
		expected.append(' ');
		expected.append(82);
		expected.append(' ');
		expected.append("TestLoggers.java");
		expected.append(' ');
		expected.append("test");
		Assert.assertEquals(expected.toString(), log);
		
		Logger logger = factory.getLogger("test");
		logger.fatal();
		logger.fatal("test", new Exception());
		logger.error();
		logger.error("test", new Exception());
		logger.warn();
		logger.warn("test", new Exception());
		logger.info();
		logger.info("test", new Exception());
		logger.debug();
		logger.debug("test", new Exception());
		logger.trace();
		logger.trace("test", new Exception());

		factory.getRoot().info("Test log message");
		
		// test rolling
		
		logger = factory.getLogger("testSmallFile");
		logger.info("A first message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A second message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A third message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A fourth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A fifth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A sixth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A seventh message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("A message to almost reach the maximum size of 1KB");
		factory.flush().blockThrow(0);
		Assert.assertTrue(new File(dir, "log_small.txt").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.1").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.2").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.3").exists());
		logger.info("/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/");
		logger.info("file2 - A first message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A second message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A third message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A fourth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A fifth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A sixth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file2 - A seventh message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		factory.flush().blockThrow(0);
		Assert.assertTrue(new File(dir, "log_small.txt").exists());
		Assert.assertTrue(new File(dir, "log_small.txt.1").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.2").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.3").exists());
		logger.info("/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/");
		logger.info("file3 - A first message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A second message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A third message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A fourth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A fifth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A sixth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file3 - A seventh message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		factory.flush().blockThrow(0);
		Assert.assertTrue(new File(dir, "log_small.txt").exists());
		Assert.assertTrue(new File(dir, "log_small.txt.1").exists());
		Assert.assertTrue(new File(dir, "log_small.txt.2").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.3").exists());
		logger.info("/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/");
		logger.info("file4 - A first message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A second message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A third message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A fourth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A fifth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A sixth message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		logger.info("file4 - A seventh message to take some space *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		factory.flush().blockThrow(0);
		Assert.assertTrue(new File(dir, "log_small.txt").exists());
		Assert.assertTrue(new File(dir, "log_small.txt.1").exists());
		Assert.assertTrue(new File(dir, "log_small.txt.2").exists());
		Assert.assertFalse(new File(dir, "log_small.txt.3").exists());
		
		new RollingFileAppender(factory, new File(dir, "toto").getAbsolutePath(), Level.DEBUG, new LogPattern("hello world"), 10, 1).close();
	}
	
	private static void produceLogs(Logger log) {
		log.fatal("this is fatal");
		log.error("this is error");
		log.warn("this is warn");
		log.info("this is info");
		log.debug("this is debug");
		log.trace("this is trace");
	}
	
	@Test
	public void testInvalidConfig() throws MalformedURLException {
		testInvalidConfig("");
		testInvalidConfig("<!-- -->");
		testInvalidConfig("<root></root>");
		testInvalidConfig("<LoggingConfiguration><unexpected/></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Appender>");
		testInvalidConfig("<LoggingConfiguration><Appender></Appender></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Appender class=\"hello\"></Appender></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Appender name=\"hello\"></Appender></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Appender name=\"hello\" class=\"" + getClass().getName() + "\"></Appender></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Logger>");
		testInvalidConfig("<LoggingConfiguration><Logger toto=\"1\"></Logger></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Logger level=\"UNKNOWN\"></Logger></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Logger name=\"test\" level=\"WARN\"></Logger></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Logger name=\"test\" level=\"WARN\" appender=\"unknown\"></Logger></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Logger name=\"test\" level=\"WARN\" appender=\"console\"><!----><unexpected/></Logger></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Default>");
		testInvalidConfig("<LoggingConfiguration><Default></Default></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Default toto=\"1\"></Default></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Default appender=\"unknown\"></Default></LoggingConfiguration>");
		testInvalidConfig("<LoggingConfiguration><Default appender=\"console\"><!----><unexpected/></Default></LoggingConfiguration>");
	}
	
	private static void testInvalidConfig(String config) throws MalformedURLException {
		URL url = new URL("string://UTF-8/" + config);
		try (InputStream in = url.openStream()) {
			LCCore.getApplication().getLoggerFactory().configure(in);
			throw new AssertionError("Error expected to configure loggers: " + config);
		} catch (Exception e) {
			// ok
		}
	}
	
}
