package net.lecousin.framework.event;

import java.util.ArrayList;

/**
 * A SimpleEvent is like an Event but does not provide data to describe the event.
 * Consequently, listeners are just Runnable.
 */
public class SimpleEvent extends AbstractSimpleListenable {

	/** Fire this event, or in other words call the listeners. */
	public void fire() {
		ArrayList<Runnable> toCall;
		synchronized (this) {
			if (this.listenersRunnable == null) return;
			toCall = new ArrayList<>(this.listenersRunnable);
		}
		for (int i = 0; i < toCall.size(); ++i)
			toCall.get(i).run();
	}
	
}
