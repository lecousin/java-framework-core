package net.lecousin.framework.concurrent.async;

/** Blockable thread. */
public interface Blockable {

	/** Signal that the current thread is blocked by the given synchronization point. */
	void blocked(IAsync<?> synchPoint, long timeout);
	
}
