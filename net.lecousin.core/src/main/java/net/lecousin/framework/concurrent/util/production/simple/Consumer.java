package net.lecousin.framework.concurrent.util.production.simple;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;

/** Consume objects or data.
 * @param <T> type of object consumed
 */
public interface Consumer<T> {

	/** Consume the given object. */
	AsyncSupplier<?,? extends Exception> consume(T product);
	
	/** Signal the end of production, meaning no new object will be given to consume. */
	AsyncSupplier<?,? extends Exception> endOfProduction();
	
	/** Signal an error and stop consumption. */
	void error(Exception error);
	
	/** Cancel and stop consumption. */
	void cancel(CancelException event);
	
}
