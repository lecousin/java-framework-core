package net.lecousin.framework.util;

import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.encoding.StringEncoding;

/**
 * IDManagerString using an IDManagerLong and a StringEncoding.
 */
public class IDManagerStringFromLong implements IDManagerString {

	/** Constructor. */
	public IDManagerStringFromLong(IDManagerLong manager, StringEncoding<Long> encoder) {
		this.manager = manager;
		this.encoder = encoder;
	}

	/** Constructor. */
	public IDManagerStringFromLong(IDManagerLong manager) {
		this(manager, new StringEncoding.SimpleLong());
	}
	
	protected IDManagerLong manager;
	protected StringEncoding<Long> encoder;
	
	@Override
	public String allocate() {
		try {
			return encoder.encode(Long.valueOf(manager.allocate()));
		} catch (EncodingException e) {
			throw new NumberFormatException(e.getMessage());
		}
	}
	
	@Override
	public void free(String id) {
		try {
			manager.free(encoder.decode(id).longValue());
		} catch (Exception e) {
			// ignore
		}
	}
	
	@Override
	public void used(String id) {
		try {
			manager.used(encoder.decode(id).longValue());
		} catch (Exception e) {
			// ignore
		}
	}
	
}
