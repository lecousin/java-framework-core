package net.lecousin.framework.io.encoding;

/** Encode and decode bytes. */
public interface IBytesEncoding {

	/** Encode the given bytes. */
	byte[] encode(byte[] bytes);

	/** Decode the given bytes. */
	byte[] decode(byte[] encoded);
	
}
