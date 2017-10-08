package net.lecousin.framework.util;

/**
 * Same as Runnable but with a parameter.
 * @param <T> type of parameter
 */
public interface RunnableWithParameter<T> {

	/** Called to execute this runnable. */
	public void run(T param);
	
}
