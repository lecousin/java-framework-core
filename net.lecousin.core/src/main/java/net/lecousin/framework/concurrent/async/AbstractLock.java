package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;

/** Base class for locks.
 * @param <TError> type of exception
 */
public abstract class AbstractLock<TError extends Exception> implements IAsync<TError> {

	protected TError error = null;
	protected CancelException cancel = null;
	protected ArrayList<Runnable> listeners = null;
	
	@Override
	public Collection<?> getAllListeners() {
		if (listeners == null) return new ArrayList<>(0);
		return new ArrayList<>(listeners);
	}
	
	protected abstract void unlock();

	@Override
	public final boolean isCancelled() { return cancel != null; }
	
	@Override
	public final void cancel(CancelException reason) {
		cancel = reason;
		unlock();
	}

	@Override
	public final CancelException getCancelEvent() {
		return cancel;
	}

	@Override
	public final boolean hasError() {
		return error != null;
	}

	@Override
	public final TError getError() {
		return error;
	}

	@Override
	public final void error(TError error) {
		this.error = error;
		unlock();
	}

}
