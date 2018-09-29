package net.lecousin.framework.util;

import java.util.Random;

import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;

/**
 * ID Manager allocating long integer identifiers.
 */
public class IDManagerRandomLong implements IDManager {

	/** Constructor.
	 * @param encoder to encode a long value into a string
	 */
	public IDManagerRandomLong(StringEncoding<Long> encoder) {
		this.encoder = encoder;
		this.rand = new Random();
	}

	/** Constructor using a default StringEncoding$SimpleLong encoder. */
	public IDManagerRandomLong() {
		this(new StringEncoding.SimpleLong());
	}

	private StringEncoding<Long> encoder;
	private Random rand;
	private FragmentedRangeLong free = new FragmentedRangeLong(new RangeLong(1,Long.MAX_VALUE));
	
	@Override
	public String allocate() {
		long n = rand.nextLong();
		long id;
		synchronized (free) {
			long max = free.getTotalSize();
			n = n % max;
			if (n < 0) n = -n;
			id = free.removeValueAt(n);
		}
		return encoder.encode(Long.valueOf(id));
	}
	
	@Override
	public void free(String id) {
		long l = encoder.decode(id).longValue();
		synchronized (free) {
			free.addValue(l);
		}
	}
	
	@Override
	public void used(String id) {
		long l = encoder.decode(id).longValue();
		synchronized (free) {
			free.removeValue(l);
		}
	}
	
}
