package net.lecousin.framework.event;

import java.util.ArrayList;
import java.util.LinkedList;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.exception.NoException;

/**
 * An async event allows to fire listeners in a separate tasks.
 * If multiple events occur before the listeners are called, only one event is fired (not queued).
 * If an event occurs while calling listeners, listeners will be called again.
 */
public class AsyncEvent implements SimpleListenable {

	private LinkedList<Runnable> listeners = new LinkedList<>();
	private Fire next = null;
	private Fire current = null;
	
	@Override
	public void addListener(Runnable listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	@Override
	public void removeListener(Runnable listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	
	@Override
	public boolean hasListeners() {
		return !listeners.isEmpty();
	}
	
	/** Fire the event, and call the listeners in a separate task. */
	public void fire() {
		synchronized (listeners) {
			if (next == null) next = new Fire();
		}
	}
	
	private class Fire extends Task.Cpu<Void, NoException> {
		private Fire() {
			super("Fire AsyncEvent listeners", Task.PRIORITY_NORMAL);
			if (current != null)
				current.getOutput().listenAsync(this, true);
			else
				start();
		}
		
		@Override
		public Void run() {
			ArrayList<Runnable> list;
			synchronized (listeners) {
				current = this;
				next = null;
				list = new ArrayList<>(listeners);
			}
			for (Runnable listener : list)
				try { listener.run(); }
				catch (Exception t) {
					LCCore.getApplication().getDefaultLogger().error("Listener of AsyncEvent thrown an exception", t);
				}
			return null;
		}
	}
	
}
