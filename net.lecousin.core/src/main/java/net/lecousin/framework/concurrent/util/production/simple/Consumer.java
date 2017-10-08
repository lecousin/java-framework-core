package net.lecousin.framework.concurrent.util.production.simple;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;

/** Consume objects or data.
 * @param <T> type of object consumed
 */
public interface Consumer<T> {

	/** Consume the given object. */
	public AsyncWork<?,? extends Exception> consume(T product);
	
	/** Signal the end of production, meaning no new object will be given to consume. */
	public AsyncWork<?,? extends Exception> endOfProduction();
	
	/** Signal an error and stop consumption. */
	public void error(Exception error);
	
	/** Cancel and stop consumption. */
	public void cancel(CancelException event);
	
}
