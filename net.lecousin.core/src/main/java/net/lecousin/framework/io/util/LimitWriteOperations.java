package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.util.LimitAsyncOperations;
import net.lecousin.framework.io.IO;

/**
 * This class allows to queue write operations, but blocks if too many are waiting.
 * This can typically used in operations reading from an IO, and writing to another, when the amount of data can be large:
 * usually read operations are faster than write operations, and we need to avoid having too much buffers in memory waiting
 * to write.
 */
public class LimitWriteOperations extends LimitAsyncOperations<ByteBuffer, Integer, IOException> {
	
	/** Constructor. */
	public LimitWriteOperations(IO.Writable io, int maxOperations) {
		super(maxOperations, io::writeAsync);
	}

}
