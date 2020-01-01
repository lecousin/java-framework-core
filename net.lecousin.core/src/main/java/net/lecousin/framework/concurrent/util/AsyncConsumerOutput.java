package net.lecousin.framework.concurrent.util;

import net.lecousin.framework.concurrent.async.AsyncSupplier;

/** AsyncConsumer on which we have an output.
 * @param <TInput> type of data consumed
 * @param <TOutput> type of the output
 * @param <TError> type of error
 */
public interface AsyncConsumerOutput<TInput, TOutput, TError extends Exception> extends AsyncConsumer<TInput, TError> {

	/** Return the output of this consumer. */
	AsyncSupplier<TOutput, TError> getOutput();
	
}
