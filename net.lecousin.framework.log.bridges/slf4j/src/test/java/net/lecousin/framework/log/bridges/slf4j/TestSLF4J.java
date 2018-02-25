package net.lecousin.framework.log.bridges.slf4j;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSLF4J extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Logger l = LoggerFactory.getLogger("test");
		l = LoggerFactory.getLogger("test");
		l.getName();
		Object o1 = Integer.valueOf(11);
		Object o2 = Integer.valueOf(22);
		Object o3 = Integer.valueOf(33);
		l.isTraceEnabled();
		l.trace("test");
		l.trace("test", o1);
		l.trace("test", o1, o2);
		l.trace("test", o1, o2, o3);
		l.trace("test", new Exception());
		l.isDebugEnabled();
		l.debug("test");
		l.debug("test", o1);
		l.debug("test", o1, o2);
		l.debug("test", o1, o2, o3);
		l.debug("test", new Exception());
		l.isInfoEnabled();
		l.info("test");
		l.info("test", o1);
		l.info("test", o1, o2);
		l.info("test", o1, o2, o3);
		l.info("test", new Exception());
		l.isWarnEnabled();
		l.warn("test");
		l.warn("test", o1);
		l.warn("test", o1, o2);
		l.warn("test", o1, o2, o3);
		l.warn("test", new Exception());
		l.isErrorEnabled();
		l.error("test");
		l.error("test", o1);
		l.error("test", o1, o2);
		l.error("test", o1, o2, o3);
		l.error("test", new Exception());
	}
	
}
