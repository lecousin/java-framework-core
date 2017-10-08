package net.lecousin.framework.log.appenders;

import net.lecousin.framework.log.LogPattern.Log;

/** Log appender. */
public interface Appender {

	/** Append the given log. */
	public void append(Log log);
	
	/** Return the level of this appender. */
	public int level();
	
	/** Return true if this appender needs the thread name. */
	public boolean needsThreadName();
	
	/** Return true if this appender needs the location. */
	public boolean needsLocation();
	
}
