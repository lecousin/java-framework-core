package net.lecousin.framework.util;

/** Utility methods for multi-threading. */
public final class ThreadUtil {

	private ThreadUtil() {
		// no instance
	}
	
	/** Call the wait method on the given lock and return false in case it has been interrupted
	 * MUST be called in a synchronized block and in a loop to check condition. */
	@SuppressWarnings({"squid:S2274", "squid:S2273"})
	public static boolean wait(Object lock, long timeout) {
		try {
			lock.wait(timeout);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	/** Pause the current thread and return false in case it has been interrupted. */
	public static boolean sleep(long timeout) {
		try {
			Thread.sleep(timeout);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
}
