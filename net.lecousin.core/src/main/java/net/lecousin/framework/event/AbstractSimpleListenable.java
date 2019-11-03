package net.lecousin.framework.event;

import java.util.ArrayList;

/** Abstract class implementaing SimpleListenable, containing a list of listeners. */
public abstract class AbstractSimpleListenable implements SimpleListenable {

	protected ArrayList<Runnable> listenersRunnable = null;

	@Override
	public synchronized void addListener(Runnable listener) {
		if (listenersRunnable == null) listenersRunnable = new ArrayList<>(5);
		listenersRunnable.add(listener);
	}

	@Override
	public synchronized void removeListener(Runnable listener) {
		if (listenersRunnable == null) return;
		listenersRunnable.remove(listener);
		if (listenersRunnable.isEmpty()) listenersRunnable = null;
	}

	@Override
	public boolean hasListeners() { return listenersRunnable != null; }
	
}
