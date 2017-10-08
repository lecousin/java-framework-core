package net.lecousin.framework.io.provider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

/** Implement IOProvider for readable and writable, for a given bytes array. */
public class ByteArrayIOProvider implements IOProvider.Readable.Seekable.DeterminedSize, IOProvider.Writable.Seekable, IOProvider.ReadWrite.Seekable {

	/** Constructor. */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
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
	public ByteArrayIO provideIOReadWriteSeekable(byte priority) {
		return new ByteArrayIO(array, bytesUsed, description);
	}

	@Override
	public IO.Writable.Seekable provideIOWritableSeekable(byte priority) {
		return new ByteArrayIO(array, bytesUsed, description);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ByteArrayIO provideIOReadableSeekableDeterminedSize(byte priority) {
		return new ByteArrayIO(array, bytesUsed, description);
	}
	
}
