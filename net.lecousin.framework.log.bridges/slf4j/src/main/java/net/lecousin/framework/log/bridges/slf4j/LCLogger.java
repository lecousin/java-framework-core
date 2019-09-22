package net.lecousin.framework.log.bridges.slf4j;

import net.lecousin.framework.log.Logger.Level;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public class LCLogger extends MarkerIgnoringBase {

	private static final long serialVersionUID = 1118460356665863775L;

	LCLogger(String name, net.lecousin.framework.log.Logger logger) {
		this.name = name;
		this.logger = logger;
	}
	
	private transient net.lecousin.framework.log.Logger logger;
	
	@Override
	public boolean isTraceEnabled() { return logger.trace(); }

	@Override
	public void trace(String msg) { logger.trace(msg); }

	@Override
	public void trace(String format, Object arg) { formatAndLog(Level.TRACE, format, arg); }

	@Override
	public void trace(String format, Object arg1, Object arg2) { formatAndLog(Level.TRACE, format, arg1, arg2); }

	@Override
	public void trace(String format, Object... arguments) { formatAndLog(Level.TRACE, format, arguments); }

	@Override
	public void trace(String msg, Throwable t) { logger.trace(msg, t); }

	@Override
	public boolean isDebugEnabled() { return logger.debug(); }

	@Override
	public void debug(String msg) { logger.debug(msg); }

	@Override
	public void debug(String format, Object arg) { formatAndLog(Level.DEBUG, format, arg); }

	@Override
	public void debug(String format, Object arg1, Object arg2) { formatAndLog(Level.DEBUG, format, arg1, arg2); }

	@Override
	public void debug(String format, Object... arguments) { formatAndLog(Level.DEBUG, format, arguments); }

	@Override
	public void debug(String msg, Throwable t) { logger.debug(msg, t); }

	@Override
	public boolean isInfoEnabled() { return logger.info(); }

	@Override
	public void info(String msg) { logger.info(msg); }

	@Override
	public void info(String format, Object arg) { formatAndLog(Level.INFO, format, arg); }

	@Override
	public void info(String format, Object arg1, Object arg2) { formatAndLog(Level.INFO, format, arg1, arg2); }

	@Override
	public void info(String format, Object... arguments) { formatAndLog(Level.INFO, format, arguments); }

	@Override
	public void info(String msg, Throwable t) { logger.info(msg, t); }

	@Override
	public boolean isWarnEnabled() { return logger.warn(); }

	@Override
	public void warn(String msg) { logger.warn(msg); }

	@Override
	public void warn(String format, Object arg) { formatAndLog(Level.WARN, format, arg); }

	@Override
	public void warn(String format, Object... arguments) { formatAndLog(Level.WARN, format, arguments); }

	@Override
	public void warn(String format, Object arg1, Object arg2) { formatAndLog(Level.WARN, format, arg1, arg2); }

	@Override
	public void warn(String msg, Throwable t) { logger.warn(msg, t); }

	@Override
	public boolean isErrorEnabled() { return logger.error(); }

	@Override
	public void error(String msg) { logger.error(msg); }

	@Override
	public void error(String format, Object arg) { formatAndLog(Level.ERROR, format, arg); }

	@Override
	public void error(String format, Object arg1, Object arg2) { formatAndLog(Level.ERROR, format, arg1, arg2); }

	@Override
	public void error(String format, Object... arguments) { formatAndLog(Level.ERROR, format, arguments); }

	@Override
	public void error(String msg, Throwable t) { logger.error(msg, t); }

    private void formatAndLog(Level level, String format, Object arg) {
    	formatAndLog(level, format, arg, null);
    }
	
    private void formatAndLog(Level level, String format, Object arg1, Object arg2) {
        FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
        logger.log(level, tp.getMessage(), tp.getThrowable());
    }

    private void formatAndLog(Level level, String format, Object... arguments) {
        FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
        logger.log(level, tp.getMessage(), tp.getThrowable());
    }
    
}
