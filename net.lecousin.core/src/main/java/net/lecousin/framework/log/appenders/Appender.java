package net.lecousin.framework.log.appenders;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.log.LogPattern.Log;

/** Log appender. */
public interface Appender {

	/** Append the given log. */
	void append(Log log);
	
	/** Return the level of this appender. */
	int level();
	
	/** Return true if this appender needs the thread name. */
	boolean needsThreadName();
	
	/** Return true if this appender needs the location. */
	boolean needsLocation();
	
	/** Ask to flush any pending log that this appender is still holding. */
	ISynchronizationPoint<Exception> flush();
	
}
