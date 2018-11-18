package net.lecousin.framework.util;

import java.util.Random;

import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;

/**
 * ID Manager allocating long integer identifiers.
 */
public class RandomIDManagerLong implements IDManagerLong {

	/** Constructor. */
	public RandomIDManagerLong() {
		this.rand = new Random();
	}

	private Random rand;
	private FragmentedRangeLong free = new FragmentedRangeLong(new RangeLong(1,Long.MAX_VALUE));
	
	@Override
	public long allocate() {
		long n = rand.nextLong();
		long id;
		synchronized (free) {
			long max = free.getTotalSize();
			n = n % max;
			if (n < 0) n = -n;
			id = free.removeValueAt(n);
		}
		return id;
	}
	
	@Override
	public void free(long id) {
		synchronized (free) {
			free.addValue(id);
		}
	}
	
	@Override
	public void used(long id) {
		synchronized (free) {
			free.removeValue(id);
		}
	}
	
}
