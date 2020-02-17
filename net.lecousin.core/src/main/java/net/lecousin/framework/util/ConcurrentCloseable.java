package net.lecousin.framework.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.event.Event;

/** Implement most of the functionalities expected by an IConcurrentCloseable.
 * @param <TError> type of error when closing.
 */
public abstract class ConcurrentCloseable<TError extends Exception> implements IConcurrentCloseable<TError> {

	private boolean open = true;
	private HashSet<IAsync<?>> pendingOperations = new HashSet<>(5);
	private Async<TError> closing = null;
	private Event<CloseableListenable> closeEvent = null;
	private int closeLocked = 0;
	private Async<TError> waitForClose = null;

	/** Return the priority. */
	public abstract byte getPriority();
	
	protected abstract IAsync<TError> closeUnderlyingResources();
	
	protected abstract void closeResources(Async<TError> ondone);
	
	public boolean isClosing() {
		return open && closing != null;
	}
	
	@Override
	public boolean isClosed() {
		return !open;
	}

	@Override
	public void addCloseListener(Consumer<CloseableListenable> listener) {
		synchronized (this) {
			if (closing == null && open) {
				if (closeEvent == null) closeEvent = new Event<>();
				closeEvent.addListener(listener);
				return;
			}
		}
		listener.accept(this);
	}
	
	@Override
	public void addCloseListener(Runnable listener) {
		synchronized (this) {
			if (closing == null && open) {
				if (closeEvent == null) closeEvent = new Event<>();
				closeEvent.addListener(listener);
				return;
			}
		}
		listener.run();
	}

	@Override
	public void removeCloseListener(Consumer<CloseableListenable> listener) {
		synchronized (this) {
			if (closeEvent == null) return;
			closeEvent.removeListener(listener);
			if (!closeEvent.hasListeners()) closeEvent = null;
		}
	}
	
	@Override
	public void removeCloseListener(Runnable listener) {
		synchronized (this) {
			if (closeEvent == null) return;
			closeEvent.removeListener(listener);
			if (!closeEvent.hasListeners()) closeEvent = null;
		}
	}
	
	@Override
	public boolean lockClose() {
		synchronized (this) {
			if (closing != null) return false;
			closeLocked++;
		}
		return true;
	}

	@Override
	public void unlockClose() {
		boolean unblock = false;
		synchronized (this) {
			if (--closeLocked == 0) unblock = waitForClose != null;
		}
		if (unblock)
			closeAsync();
	}
	
	@Override
	public void close() throws Exception {
		if (closeLocked > 0) return;
		closeAsync();
		closing.blockThrow(0);
	}
	
	@Override
	public IAsync<TError> closeAsync() {
		synchronized (this) {
			if (closeLocked > 0) {
				if (waitForClose == null) waitForClose = new Async<>();
				return waitForClose;
			}
			if (closing != null) return closing;
			closing = new Async<>();
		}
		// from now, no more operation is accepted
		byte prio = getPriority();
		JoinPoint<TError> jp = new JoinPoint<>();
		List<IAsync<?>> pending;
		synchronized (pendingOperations) {
			pending = new ArrayList<>(pendingOperations);
			pendingOperations.clear();
		}
		for (IAsync<?> op : pending)
			jp.addToJoinNoException(op);
		IAsync<TError> underlying = closeUnderlyingResources();
		if (underlying != null)
			jp.addToJoinNoException(underlying);
		jp.start();
		jp.thenStart(new Task.Cpu.FromRunnable("Closing resources", prio, () -> {
			synchronized (this) {
				open = false;
			}
			if (closeEvent != null) {
				closeEvent.fire(this);
				closeEvent = null;
			}
			Async<TError> closed = new Async<>();
			closeResources(closed);
			closed.onDone(() -> {
				if (jp.forwardIfNotSuccessful(closing))
					return;
				if (underlying != null && underlying.forwardIfNotSuccessful(closing))
					return;
				closing.unblock();
			}, closing);
		}), true);
		jp.listenTime(60000, () -> {
			StringBuilder s = new StringBuilder();
			s.append("Closeable still waiting for pending operations: ").append(this);
			for (IAsync<?> op : pending)
				if (!op.isDone()) {
					s.append("\r\n - ").append(op);
					for (Object o : op.getAllListeners())
						s.append("\r\n    - ").append(o);
				}
			if (underlying != null && !underlying.isDone())
				s.append("\r\n - closeUnderlyingResources");
			LCCore.getApplication().getDefaultLogger().error(s.toString());
			jp.cancel(new CancelException("Closeable still waiting for pending operations after 1 minute, close forced"));
		});
		if (waitForClose != null)
			closing.onDone(waitForClose);
		return closing;
	}
	
	private static CancelException createCancellation() {
		return new CancelException("Resource closed");
	}
	
	protected <TE extends Exception, T extends IAsync<TE>> T operation(T op) {
		if (op.isDone()) return op;
		if (closing != null) {
			op.cancel(createCancellation());
			return op; 
		}
		synchronized (pendingOperations) {
			if (closing == null)
				pendingOperations.add(op);
		}
		if (closing != null) {
			op.cancel(createCancellation());
			return op; 
		}
		op.onDone(() -> {
			synchronized (pendingOperations) {
				pendingOperations.remove(op);
			}
		});
		return op;
	}
	
	protected <TE extends Exception, TR, T extends Task<TR,TE>> T operation(T task) {
		operation(task.getOutput());
		return task;
	}
	
}
