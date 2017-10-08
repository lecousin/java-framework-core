package net.lecousin.framework.util;

/**
 * Simple factory.
 * @param <T> type of object this factory creates.
 */
public interface InstanceFactory<T> {

	/** Create a new instance of T. */
	public T create();
	
}
