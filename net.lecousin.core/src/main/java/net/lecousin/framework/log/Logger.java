package net.lecousin.framework.log;

import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.appenders.Appender;

/** Logger. */
public class Logger {
	
	/** Level of logging. */
	public enum Level {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL,
		OFF;
	}

	Logger(LoggerFactory factory, String name, Appender appender) {
		this.factory = factory;
		this.name = name;
		this.appender = appender;
	}
	
	private LoggerFactory factory;
	private String name;
	Appender appender;
	
	private void logMessage(Level level, String message, Throwable t) {
		Log log = new Log();
		log.timestamp = System.currentTimeMillis();
		log.loggerName = name;
		log.level = level;
		log.message = message;
		log.trace = t;
		if (appender.needsThreadName())
			log.threadName = Thread.currentThread().getName();
		if (appender.needsLocation()) {
			StackTraceElement[] stack = new Exception().getStackTrace();
			if (stack != null && stack.length > 1) {
				log.location = stack[1];
			}
		}
		factory.thread.log(appender, log);
	}
	
	/** Log. */
	public void log(Level level, String message) {
		if (level.ordinal() < appender.level()) return;
		logMessage(level, message, null);
	}

	/** Log. */
	public void log(Level level, String message, Throwable t) {
		if (level.ordinal() < appender.level()) return;
		logMessage(level, message, t);
	}
	
	public boolean trace() { return appender.level() <= Level.TRACE.ordinal(); }
	
	/** Log. */
	public void trace(String message) {
		if (appender.level() > Level.TRACE.ordinal()) return;
		logMessage(Level.TRACE, message, null);
	}

	/** Log. */
	public void trace(String message, Throwable t) {
		if (appender.level() > Level.TRACE.ordinal()) return;
		logMessage(Level.TRACE, message, t);
	}
	
	public boolean debug() { return appender.level() <= Level.DEBUG.ordinal(); }
	
	/** Log. */
	public void debug(String message) {
		if (appender.level() > Level.DEBUG.ordinal()) return;
		logMessage(Level.DEBUG, message, null);
	}

	/** Log. */
	public void debug(String message, Throwable t) {
		if (appender.level() > Level.DEBUG.ordinal()) return;
		logMessage(Level.DEBUG, message, t);
	}
	
	public boolean info() { return appender.level() <= Level.INFO.ordinal(); }
	
	/** Log. */
	public void info(String message) {
		if (appender.level() > Level.INFO.ordinal()) return;
		logMessage(Level.INFO, message, null);
	}

	/** Log. */
	public void info(String message, Throwable t) {
		if (appender.level() > Level.INFO.ordinal()) return;
		logMessage(Level.INFO, message, t);
	}
	
	public boolean warn() { return appender.level() <= Level.WARN.ordinal(); }
	
	/** Log. */
	public void warn(String message) {
		if (appender.level() > Level.WARN.ordinal()) return;
		logMessage(Level.WARN, message, null);
	}

	/** Log. */
	public void warn(String message, Throwable t) {
		if (appender.level() > Level.WARN.ordinal()) return;
		logMessage(Level.WARN, message, t);
	}
	
	public boolean error() { return appender.level() <= Level.ERROR.ordinal(); }

	/** Log. */
	public void error(String message) {
		if (appender.level() > Level.ERROR.ordinal()) return;
		logMessage(Level.ERROR, message, null);
	}

	/** Log. */
	public void error(String message, Throwable t) {
		if (appender.level() > Level.ERROR.ordinal()) return;
		logMessage(Level.ERROR, message, t);
	}
	
	public boolean fatal() { return appender.level() <= Level.FATAL.ordinal(); }

	/** Log. */
	public void fatal(String message) {
		if (appender.level() > Level.FATAL.ordinal()) return;
		logMessage(Level.FATAL, message, null);
	}

	/** Log. */
	public void fatal(String message, Throwable t) {
		if (appender.level() > Level.FATAL.ordinal()) return;
		logMessage(Level.FATAL, message, t);
	}
	
}
