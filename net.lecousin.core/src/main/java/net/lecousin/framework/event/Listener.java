package net.lecousin.framework.event;

/** A Listener is an object that can be called with an object as parameter.
 * It is typically used with events, but can be used anywhere in a similar way as a Runnable
 * but with a parameter.
 * @param <T> type of data
 */
public interface Listener<T> {

	/** Called when the event occurs. */
	void fire(T event);
	
	/** Abstract class implementing Listener and holding an object given at instantiation time.
	 * @param <TEvent> type of data fired
	 * @param <TData> type of data hold
	 */
	public abstract static class WithData<TEvent,TData> implements Listener<TEvent> {
		/** Constructor with the data to hold. */
		public WithData(TData data) {
			this.data = data;
		}
		
		protected TData data;
		
		public TData getData() { return data; }
	}
	
}
