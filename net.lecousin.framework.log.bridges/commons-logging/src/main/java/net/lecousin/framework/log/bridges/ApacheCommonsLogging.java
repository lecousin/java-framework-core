package net.lecousin.framework.log.bridges;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.log.Logger;

public class ApacheCommonsLogging implements org.apache.commons.logging.Log {

	public ApacheCommonsLogging(String name) {
		lc = LCCore.getApplication().getLoggerFactory().getLogger(name);
	}
	
	private Logger lc;

	@Override
	public void debug(Object arg0) {
		lc.debug(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void debug(Object arg0, Throwable arg1) {
		lc.debug(arg0 != null ? arg0.toString() : "null", arg1);
	}

	@Override
	public void error(Object arg0) {
		lc.error(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void error(Object arg0, Throwable arg1) {
		lc.error(arg0 != null ? arg0.toString() : "null", arg1);
	}

	@Override
	public void fatal(Object arg0) {
		lc.fatal(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void fatal(Object arg0, Throwable arg1) {
		lc.fatal(arg0 != null ? arg0.toString() : "null", arg1);
	}

	@Override
	public void info(Object arg0) {
		lc.info(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void info(Object arg0, Throwable arg1) {
		lc.info(arg0 != null ? arg0.toString() : "null", arg1);
	}

	@Override
	public boolean isDebugEnabled() {
		return lc.debug();
	}

	@Override
	public boolean isErrorEnabled() {
		return lc.error();
	}

	@Override
	public boolean isFatalEnabled() {
		return lc.fatal();
	}

	@Override
	public boolean isInfoEnabled() {
		return lc.info();
	}

	@Override
	public boolean isTraceEnabled() {
		return lc.trace();
	}

	@Override
	public boolean isWarnEnabled() {
		return lc.warn();
	}

	@Override
	public void trace(Object arg0) {
		lc.trace(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void trace(Object arg0, Throwable arg1) {
		lc.trace(arg0 != null ? arg0.toString() : "null", arg1);
	}

	@Override
	public void warn(Object arg0) {
		lc.warn(arg0 != null ? arg0.toString() : "null");
	}

	@Override
	public void warn(Object arg0, Throwable arg1) {
		lc.warn(arg0 != null ? arg0.toString() : "null", arg1);
	}
	
}
