package net.lecousin.framework.io.provider;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

/** Implement IOProvider for readable and writable, for a given bytes array. */
public class ByteArrayIOProvider implements IOProvider.ReadWrite.Seekable.KnownSize {

	/** Constructor. */
	public ByteArrayIOProvider(byte[] array, int bytesUsed, String description) {
		this.array = array;
		this.bytesUsed = bytesUsed;
		this.description = description;
	}
	
	/** Constructor. */
	public ByteArrayIOProvider(byte[] array, String description) {
		this(array, array.length, description);
	}
	
	private byte[] array;
	private int bytesUsed;
	private String description;

	@Override
	public String getDescription() {
		return description;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize>
	T provideIOReadWriteSeekableKnownSize(byte priority) {
		return (T)new ByteArrayIO(array, bytesUsed, description);
	}

}
