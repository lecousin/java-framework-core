package net.lecousin.framework.util;

/** A filter return true on an element if it accepts it.
 * @param <T> type of elements to be filtered
 */
public interface Filter<T> {

	/** Return true if the element should be kept in output of this filter. */
	public boolean accept(T element);
	
	/** Simple implementation that accept only one value.
	 * @param <T> type of elements to be filtered
	 */
	public class Single<T> implements Filter<T> {
		
		/** Constructor. */
		public Single(T value) {
			this.value = value;
		}
		
		private T value;
		
		@Override
		public boolean accept(T element) {
			return ObjectUtil.equalsOrNull(value, element);
		}
		
	}
	
}
