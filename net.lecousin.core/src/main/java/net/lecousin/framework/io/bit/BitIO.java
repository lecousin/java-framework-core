package net.lecousin.framework.io.bit;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
		
		/** Asynchronous method to read the next bit.
		 * The default implementation is using readBooleanIfAvailable and return an AsyncSupplier already unblocked
		 * if the bit is available. If the bit is not yet available it retries once
		 * the result of the method {@link #canStartReading()} is unblocked.
		 */
		default AsyncSupplier<Boolean, IOException> readBooleanAsync() {
			AsyncSupplier<Boolean, IOException> result = new AsyncSupplier<>();
			Consumer<AsyncSupplier<Boolean, IOException>> doIt = new Consumer<AsyncSupplier<Boolean, IOException>>() {
				@Override
				public void accept(AsyncSupplier<Boolean, IOException> result) {
					try {
						Boolean b = readBooleanIfAvailable();
						if (b != null) {
							result.unblockSuccess(b);
							return;
						}
					} catch (IOException e) {
						result.error(e);
						return;
					}
					canStartReading().onDone(() -> this.accept(result), result);
				}
			};
			doIt.accept(result);
			return result;
		}
		
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
		 * The default implementation is using readBitsIfAvailable and return an AsyncSupplier already unblocked
		 * if the bits are available. If the bits are not yet available it retries once
		 * the result of the method {@link #canStartReading()} is unblocked.
		 */
		default AsyncSupplier<Long, IOException> readBitsAsync(int n) {
			AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
			BiConsumer<Integer, AsyncSupplier<Long, IOException>> doIt = new BiConsumer<Integer, AsyncSupplier<Long, IOException>>() {
				@Override
				public void accept(Integer n, AsyncSupplier<Long, IOException> result) {
					try {
						long val = readBitsIfAvailable(n.intValue());
						if (val != -1) {
							result.unblockSuccess(Long.valueOf(val));
							return;
						}
					} catch (IOException e) {
						result.error(e);
						return;
					}
					canStartReading().onDone(() -> this.accept(n, result), result);
				}
			};
			doIt.accept(Integer.valueOf(n), result);
			return result;
		}
		
	}
	
}
