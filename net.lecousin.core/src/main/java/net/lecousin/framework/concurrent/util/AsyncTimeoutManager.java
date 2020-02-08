package net.lecousin.framework.concurrent.util;

import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;

/** Watch for asynchronous operaions timeout. */
public final class AsyncTimeoutManager {

	/** Watch the given asynchronous operation, and call onTimeout if not done after timeout. */
	public static void timeout(IAsync<?> async, int timeout, Runnable onTimeout) {
		Application app = LCCore.getApplication();
		AsyncTimeoutManager manager;
		synchronized (app) {
			manager = app.getInstance(AsyncTimeoutManager.class);
			if (manager == null) {
				manager = new AsyncTimeoutManager();
				app.setInstance(AsyncTimeoutManager.class, manager);
			}
		}
		manager.add(async, timeout, onTimeout);
	}
	
	private AsyncTimeoutManager() {
	}
	
	private TimeoutTask task = null;
	private long taskTime = 0;
	private LinkedList<Waiting> waiting = new LinkedList<>();
	
	private static class Waiting {
		private IAsync<?> async;
		private long expiration;
		private Runnable onTimeout;
	}
	
	private void add(IAsync<?> async, int timeout, Runnable onTimeout) {
		if (async.isDone()) return;
		Waiting w = new Waiting();
		w.async = async;
		w.onTimeout = onTimeout;
		w.expiration = System.currentTimeMillis() + timeout;
		synchronized (waiting) {
			waiting.add(w);
			updateTask(w.expiration);
		}
		async.onDone(() -> {
			synchronized (waiting) {
				if (waiting.remove(w))
					updateTask();
			}
		});
	}
	
	private void updateTask(long expiration) {
		if (task == null) {
			taskTime = expiration;
			task = new TimeoutTask();
			task.executeAt(expiration);
			task.start();
		} else if (expiration < taskTime) {
			taskTime = expiration;
			task.changeNextExecutionTime(expiration);
		}
	}
	
	private void updateTask() {
		long next = -1;
		for (Waiting w : waiting)
			if (next == -1 || w.expiration < next)
				next = w.expiration;
		if (next == -1) {
			if (task != null) {
				task.cancelIfExecutionNotStarted(new CancelException("No more timeout to watch"));
				task = null;
			}
		} else if (next != taskTime) {
			taskTime = next;
			task.changeNextExecutionTime(next);
		}
	}
	
	private class TimeoutTask extends Task.Cpu<Void, NoException> {
		private TimeoutTask() {
			super("Watch asynchronous operations timeout", Task.PRIORITY_LOW);
		}
		
		@Override
		public Void run() throws NoException, CancelException {
			long next = -1;
			LinkedList<Runnable> toRun = new LinkedList<>();
			long now = System.currentTimeMillis();
			synchronized (waiting) {
				for (Iterator<Waiting> it = waiting.iterator(); it.hasNext(); ) {
					Waiting w = it.next();
					if (w.expiration <= now) {
						if (!w.async.isDone())
							toRun.add(w.onTimeout);
						it.remove();
					} else if (next == -1 || w.expiration < next) {
						next = w.expiration;
					}
				}
				taskTime = next;
				if (next != -1)
					executeAgainAt(next);
			}
			for (Runnable r : toRun)
				r.run();
			return null;
		}
	}
	
}
