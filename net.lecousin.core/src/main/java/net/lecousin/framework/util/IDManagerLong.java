package net.lecousin.framework.util;

import net.lecousin.framework.io.encoding.IBytesEncoding;
import net.lecousin.framework.io.util.DataUtil;
import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;

/**
 * ID Manager allocating long integer identifiers.
 */
public class IDManagerLong implements IDManager {

	/** Constructor.
	 * @param encoder to encode a long value into a string
	 */
	public IDManagerLong(IBytesEncoding encoder) {
		this.encoder = encoder;
	}
	
	private IBytesEncoding encoder;
	private FragmentedRangeLong free = new FragmentedRangeLong(new RangeLong(1,Long.MAX_VALUE));
	
	@Override
	public String allocate() {
		Long id = free.removeFirstValue();
		return new String(encoder.encode(DataUtil.getBytesLittleEndian(id.longValue())));
	}
	
	@Override
	public void free(String id) {
		long l = DataUtil.readLongLittleEndian(encoder.decode(id.getBytes()), 0);
		free.addValue(l);
	}
	
}
