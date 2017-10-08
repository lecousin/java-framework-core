package net.lecousin.framework.io.encoding;

/** Encode and decode bytes. */
public interface IBytesEncoding {

	/** Encode the given bytes. */
	public byte[] encode(byte[] bytes);

	/** Decode the given bytes. */
	public byte[] decode(byte[] encoded);
	
}
