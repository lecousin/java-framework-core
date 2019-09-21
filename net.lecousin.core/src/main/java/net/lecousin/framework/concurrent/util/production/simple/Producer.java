package net.lecousin.framework.concurrent.util.production.simple;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;

/** Produce objects or data.
 * @param <T> type of object produced
 */
public interface Producer<T> {

	/** Ask to produce a new object. */
	AsyncWork<T,? extends Exception> produce(Production<T> production);
	
	/** Cancel any pending production and stop it. */
	void cancel(CancelException event);
	
}
