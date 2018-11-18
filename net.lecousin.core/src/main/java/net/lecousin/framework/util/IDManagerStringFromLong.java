package net.lecousin.framework.util;

public class IDManagerStringFromLong implements IDManagerString {

	public IDManagerStringFromLong(IDManagerLong manager, StringEncoding<Long> encoder) {
		this.manager = manager;
		this.encoder = encoder;
	}

	public IDManagerStringFromLong(IDManagerLong manager) {
		this(manager, new StringEncoding.SimpleLong());
	}
	
	protected IDManagerLong manager;
	protected StringEncoding<Long> encoder;
	
	@Override
	public String allocate() {
		return encoder.encode(Long.valueOf(manager.allocate()));
	}
	
	@Override
	public void free(String id) {
		manager.free(encoder.decode(id).longValue());
	}
	
	@Override
	public void used(String id) {
		manager.used(encoder.decode(id).longValue());
	}
	
}
