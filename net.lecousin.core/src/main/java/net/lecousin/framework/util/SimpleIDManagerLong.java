package net.lecousin.framework.util;

import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;

/**
 * ID Manager allocating long integer identifiers.
 */
public class SimpleIDManagerLong implements IDManagerLong {

	/** Constructor. */
	public SimpleIDManagerLong() {
		// nothing to do
	}

	private FragmentedRangeLong free = new FragmentedRangeLong(new RangeLong(1,Long.MAX_VALUE));
	
	@Override
	public long allocate() {
		return free.removeFirstValue().longValue();
	}
	
	@Override
	public void free(long id) {
		free.addValue(id);
	}
	
	@Override
	public void used(long id) {
		free.removeValue(id);
	}
	
}
