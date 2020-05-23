package net.lecousin.framework.log;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.threads.Task;
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

	Logger(LoggerFactory factory, Logger parent, String name, Appender appender, Level level) {
		this.factory = factory;
		this.name = name;
		this.appender = appender;
		this.level = level != null ? level.ordinal() : -1;
		this.parent = parent;
	}
	
	private LoggerFactory factory;
	private String name;
	private Appender appender;
	private int level;
	private Logger parent;
	private Map<String, Logger> children = new HashMap<>(10);
	
	/** Change the level of this Logger, or null to set it to the default level of its appender. */
	public void setLevel(Level level) {
		this.level = level != null ? level.ordinal() : -1;
	}
	
	void setAppender(Appender appender) {
		this.appender = appender;
	}
	
	private int getLevel() {
		if (level != -1)
			return level;
		if (appender != null)
			return appender.level();
		return parent.getLevel();
	}
	
	Appender getAppender() {
		if (appender != null)
			return appender;
		return parent.getAppender();
	}
	
	Logger createChild(String parentPrefix, String subName, Appender appender, Level level) {
		int i = subName.indexOf('.', 1);
		char sep = '.';
		if (i < 0) {
			i = subName.indexOf('$', 1);
			sep = '$';
		}
		if (i > 0) {
			String childName = subName.substring(0, i);
			Logger child = children.get(childName);
			if (child == null) {
				child = new Logger(factory, this, parentPrefix + childName, null, null);
				children.put(childName, child);
			}
			return child.createChild(parentPrefix + childName + sep, subName.substring(i + 1), appender, level);
		}
		Logger child = new Logger(factory, this, parentPrefix + subName, appender, level);
		children.put(subName, child);
		return child;
	}
	
	private void logMessage(Level level, String message, Throwable t) {
		Log log = new Log();
		log.timestamp = System.currentTimeMillis();
		log.loggerName = name;
		log.level = level;
		log.message = message;
		log.trace = t;
		log.app = factory.application;
		log.context = Task.getCurrentContext();
		Appender appendr = getAppender();
		if (appendr.filter(log))
			return;
		if (appendr.needsThreadName())
			log.threadName = Thread.currentThread().getName();
		if (appendr.needsLocation()) {
			StackTraceElement[] stack = new Exception().getStackTrace();
			if (stack != null) {
				for (int i = 0; i < stack.length; ++i) {
					if (Logger.class.getName().equals(stack[i].getClassName())) continue;
					log.location = stack[i];
					break;
				}
			}
		}
		String[] neededContexts = appendr.neededContexts();
		if (neededContexts != null) {
			log.contextsValues = new String[neededContexts.length];
			for (int i = 0; i < neededContexts.length; ++i) {
				Object o = log.context != null ? log.context.getAttribute(neededContexts[i]) : "";
				log.contextsValues[i] = o != null ? o.toString() : "";
			}
		}
		factory.thread.log(appendr, log);
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
