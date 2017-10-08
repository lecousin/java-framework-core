package net.lecousin.framework.util;

/**
 * A simple abstract class that implements Runnable and holds an object given in the constructor.
 * @param <T> type of object to hold
 */
public abstract class RunnableWithData<T> implements Runnable {

	/** Constructor. */
	public RunnableWithData(T data) {
		this.data = data;
	}
	
	protected T data;
	
	public T getData() { return data; }
	
}
