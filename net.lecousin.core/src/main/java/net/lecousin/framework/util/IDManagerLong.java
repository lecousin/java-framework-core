package net.lecousin.framework.util;

import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;

/**
 * ID Manager allocating long integer identifiers.
 */
public class IDManagerLong implements IDManager {

	/** Constructor.
	 * @param encoder to encode a long value into a string
	 */
	public IDManagerLong(StringEncoding<Long> encoder) {
		this.encoder = encoder;
	}

	/** Constructor using a default StringEncoding$SimpleLong encoder. */
	public IDManagerLong() {
		this(new StringEncoding.SimpleLong());
	}

	private StringEncoding<Long> encoder;
	private FragmentedRangeLong free = new FragmentedRangeLong(new RangeLong(1,Long.MAX_VALUE));
	
	@Override
	public String allocate() {
		Long id = free.removeFirstValue();
		return encoder.encode(id);
	}
	
	@Override
	public void free(String id) {
		long l = encoder.decode(id).longValue();
		free.addValue(l);
	}
	
	@Override
	public void used(String id) {
		long l = encoder.decode(id).longValue();
		free.removeValue(l);
	}
	
}
