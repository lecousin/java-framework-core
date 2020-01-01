package net.lecousin.framework.io.bit;

import java.io.IOException;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;

/**
 * IO with bit operations.
 */
public interface BitIO extends IO {
	
	/** BitIO with the Most Significant Bits first (network order or big endian). */
	interface BigEndian extends BitIO {}

	/** BitIO with the Less Significant Bits first (little endian). */
	interface LittleEndian extends BitIO {}
	
	/**
	 * IO with operations to read bits.
	 */
	interface Readable extends BitIO {
		
		/** Check if new bytes can be read from the underlying IO. */
		IAsync<IOException> canStartReading();

		/** Synchronous method to read the next bit. */
		boolean readBoolean() throws IOException;
		
		/** Return the next bit or null if not available. */
		Boolean readBooleanIfAvailable() throws IOException;
		
		/** Asynchronous method to read the next bit. */
		AsyncSupplier<Boolean, IOException> readBooleanAsync();
		
		/** Read n bits.
		 * The maximum value for n is 55.
		 */
		long readBits(int n) throws IOException;
		
		/** Read n bits.
		 * The maximum value for n is 55.
		 * -1 is returned if not yet available.
		 */
		long readBitsIfAvailable(int n) throws IOException;
		
		/** Read n bits asynchronously.
		 * The maximum value for n is 55.
		 */
		AsyncSupplier<Long, IOException> readBitsAsync(int n);
		
	}
	
}
