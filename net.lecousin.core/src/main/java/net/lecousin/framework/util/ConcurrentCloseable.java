package net.lecousin.framework.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.Event;
import net.lecousin.framework.event.Listener;

/** Implement most of the functionalities expected by an IConcurrentCloseable. */
public abstract class ConcurrentCloseable implements IConcurrentCloseable {

	private boolean open = true;
	private HashSet<ISynchronizationPoint<?>> pendingOperations = new HashSet<>(5);
	private SynchronizationPoint<Exception> closing = null;
	private Event<CloseableListenable> closeEvent = null;
	private int closeLocked = 0;
	private SynchronizationPoint<Exception> waitForClose = null;

	/** Return the priority. */
	public abstract byte getPriority();
	
	protected abstract ISynchronizationPoint<?> closeUnderlyingResources();
	
	protected abstract void closeResources(SynchronizationPoint<Exception> ondone);
	
	public boolean isClosing() {
		return open && closing != null;
	}
	
	@Override
	public boolean isClosed() {
		return !open;
	}

	@Override
	public void addCloseListener(Listener<CloseableListenable> listener) {
		synchronized (this) {
			if (closing == null && open) {
				if (closeEvent == null) closeEvent = new Event<>();
				closeEvent.addListener(listener);
				return;
			}
		}
		listener.fire(this);
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
	public void removeCloseListener(Listener<CloseableListenable> listener) {
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
	public ISynchronizationPoint<Exception> closeAsync() {
		synchronized (this) {
			if (closeLocked > 0) {
				if (waitForClose == null) waitForClose = new SynchronizationPoint<>();
				return waitForClose;
			}
			if (closing != null) return closing;
			closing = new SynchronizationPoint<>();
		}
		// from now, no more operation is accepted
		byte prio = getPriority();
		JoinPoint<Exception> jp = new JoinPoint<>();
		List<ISynchronizationPoint<?>> pending;
		synchronized (pendingOperations) {
			pending = new ArrayList<>(pendingOperations);
			pendingOperations.clear();
		}
		for (ISynchronizationPoint<?> op : pending)
			jp.addToJoinNoException(op);
		ISynchronizationPoint<?> underlying = closeUnderlyingResources();
		if (underlying != null)
			jp.addToJoinNoException(underlying);
		jp.start();
		jp.listenAsync(new Task.Cpu.FromRunnable("Closing resources", prio, () -> {
			synchronized (this) {
				open = false;
			}
			if (closeEvent != null) {
				closeEvent.fire(this);
				closeEvent = null;
			}
			closeResources(closing);
		}), true);
		jp.listenTime(60000, () -> {
			StringBuilder s = new StringBuilder();
			s.append("Closeable still waiting for pending operations: ").append(this);
			for (ISynchronizationPoint<?> op : pending)
				if (!op.isUnblocked()) {
					s.append("\r\n - ").append(op);
					for (Object o : op.getAllListeners())
						s.append("\r\n    - ").append(o);
				}
			if (underlying != null && !underlying.isUnblocked())
				s.append("\r\n - closeUnderlyingResources");
			LCCore.getApplication().getDefaultLogger().error(s.toString());
			jp.error(new Exception("Closeable still waiting for pending operations after 1 minute, close forced"));
		});
		if (waitForClose != null)
			closing.listenInline(waitForClose);
		return closing;
	}
	
	private static CancelException createCancellation() {
		return new CancelException("Resource closed");
	}
	
	protected <T extends ISynchronizationPoint<?>> T operation(T op) {
		if (op.isUnblocked()) return op;
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
		op.listenInline(() -> {
			synchronized (pendingOperations) {
				pendingOperations.remove(op);
			}
		});
		return op;
	}
	
	protected <T extends Task<?,?>> T operation(T task) {
		operation(task.getOutput());
		return task;
	}
	
}
