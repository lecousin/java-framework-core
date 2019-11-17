package net.lecousin.framework.event;

/** Class that implements SimpleListenable and delegate to an event of type T which is lazily instantiated on its first listener.
 * @param <T> type of event
 */
public abstract class SimpleListenableContainer<T extends SimpleListenable> implements SimpleListenable {

	protected T event;
	
	protected abstract T createEvent();

	@Override
	public void addListener(Runnable listener) {
		synchronized (this) {
			if (event == null) event = createEvent();
			event.addListener(listener);
		}
	}

	@Override
	public void removeListener(Runnable listener) {
		synchronized (this) {
			if (event == null) return;
			event.removeListener(listener);
		}
	}

	@Override
	public boolean hasListeners() {
		return event.hasListeners();
	}

}
