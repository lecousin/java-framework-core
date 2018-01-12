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

	Logger(LoggerFactory factory, String name, Appender appender, Level level) {
		this.factory = factory;
		this.name = name;
		this.appender = appender;
		this.level = level != null ? level.ordinal() : -1;
	}
	
	private LoggerFactory factory;
	private String name;
	Appender appender;
	private int level;
	
	/** Change the level of this Logger, or null to set it to the default level of its appender. */
	public void setLevel(Level level) {
		this.level = level != null ? level.ordinal() : -1;
	}
	
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
			if (stack != null) {
				for (int i = 0; i < stack.length; ++i) {
					if (Logger.class.getName().equals(stack[i].getClassName())) continue;
					log.location = stack[i];
					break;
				}
			}
		}
		factory.thread.log(appender, log);
	}
	
	private int getLevel() {
		if (level == -1)
			return appender.level();
		return level;
	}
	
	/** Log. */
	public void log(Level level, String message) {
		if (level.ordinal() < getLevel()) return;
		logMessage(level, message, null);
	}

	/** Log. */
	public void log(Level level, String message, Throwable t) {
		if (level.ordinal() < getLevel()) return;
		logMessage(level, message, t);
	}
	
	public boolean trace() { return getLevel() <= Level.TRACE.ordinal(); }
	
	/** Log. */
	public void trace(String message) {
		if (getLevel() > Level.TRACE.ordinal()) return;
		logMessage(Level.TRACE, message, null);
	}

	/** Log. */
	public void trace(String message, Throwable t) {
		if (getLevel() > Level.TRACE.ordinal()) return;
		logMessage(Level.TRACE, message, t);
	}
	
	public boolean debug() { return getLevel() <= Level.DEBUG.ordinal(); }
	
	/** Log. */
	public void debug(String message) {
		if (getLevel() > Level.DEBUG.ordinal()) return;
		logMessage(Level.DEBUG, message, null);
	}

	/** Log. */
	public void debug(String message, Throwable t) {
		if (getLevel() > Level.DEBUG.ordinal()) return;
		logMessage(Level.DEBUG, message, t);
	}
	
	public boolean info() { return getLevel() <= Level.INFO.ordinal(); }
	
	/** Log. */
	public void info(String message) {
		if (getLevel() > Level.INFO.ordinal()) return;
		logMessage(Level.INFO, message, null);
	}

	/** Log. */
	public void info(String message, Throwable t) {
		if (getLevel() > Level.INFO.ordinal()) return;
		logMessage(Level.INFO, message, t);
	}
	
	public boolean warn() { return getLevel() <= Level.WARN.ordinal(); }
	
	/** Log. */
	public void warn(String message) {
		if (getLevel() > Level.WARN.ordinal()) return;
		logMessage(Level.WARN, message, null);
	}

	/** Log. */
	public void warn(String message, Throwable t) {
		if (getLevel() > Level.WARN.ordinal()) return;
		logMessage(Level.WARN, message, t);
	}
	
	public boolean error() { return getLevel() <= Level.ERROR.ordinal(); }

	/** Log. */
	public void error(String message) {
		if (getLevel() > Level.ERROR.ordinal()) return;
		logMessage(Level.ERROR, message, null);
	}

	/** Log. */
	public void error(String message, Throwable t) {
		if (getLevel() > Level.ERROR.ordinal()) return;
		logMessage(Level.ERROR, message, t);
	}
	
	public boolean fatal() { return getLevel() <= Level.FATAL.ordinal(); }

	/** Log. */
	public void fatal(String message) {
		if (getLevel() > Level.FATAL.ordinal()) return;
		logMessage(Level.FATAL, message, null);
	}

	/** Log. */
	public void fatal(String message, Throwable t) {
		if (getLevel() > Level.FATAL.ordinal()) return;
		logMessage(Level.FATAL, message, t);
	}
	
}
