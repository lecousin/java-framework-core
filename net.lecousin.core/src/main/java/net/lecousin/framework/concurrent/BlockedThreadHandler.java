package net.lecousin.framework.concurrent;

import net.lecousin.framework.concurrent.async.IAsync;

/**
 * Handle a blocking situation.
 * It is used by the synchronization points to signal to the multi-threading system
 * that the thread is being blocked, so a spare thread can be started to handle new
 * tasks.
 */
public interface BlockedThreadHandler {

	/** Singal that the current thread is blocked by the given synchronization point. */
	void blocked(IAsync<?> synchPoint, long timeout);
	
}
