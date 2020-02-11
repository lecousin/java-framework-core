package net.lecousin.framework.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.concurrent.util.AsyncProducer;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.util.IConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Base interface for all IO, see the package documentation {@link net.lecousin.framework.io}.
 */
public interface IO extends IConcurrentCloseable<IOException> {
	
	/** Describe the IO, which can be used for logging or debugging purposes. */
	String getSourceDescription();

	/** If this IO is wrapping another one (such as adding Buffered capabilities), this method returns it,
	 * or null if it does not wrap another IO.
	 */
	IO getWrappedIO();
	
	/** Synchronous ou asynchronous operation. */
	public enum OperationType {
		SYNCHRONOUS, ASYNCHRONOUS
	}
	
	/** Return the priority of asynchronous operations. */
	byte getPriority();

	/** Set the priority of asynchronous operations. */
	void setPriority(byte priority);
	
	/** Return the TaskManager used for asynchronous operations. */
	TaskManager getTaskManager();
	
	/** Capability to get the current position on an IO. */
	public interface PositionKnown extends IO {
		/** Return the current position. */
		long getPosition() throws IOException;
	}
	
	/** Capability to set the position on an IO. */
	public interface Seekable extends PositionKnown {
		/** List of possible seek operations. */
		public enum SeekType {
			/** Set the position based on the beginning of the IO. */
			FROM_BEGINNING,
			/** Set the position based on the current position of the IO. */
			FROM_CURRENT,
			/** Set the position based on the end of the IO. */
			FROM_END
		}
		
		/** Returns the new position.
		 * The implementation must not allow to seek after the end of the file, the method setSize should be used instead.
		 * It must accept seeking beyond the end, but position at the end,
		 * and accept seeking before the beginning, but position at the beginning.
		 */
		long seekSync(SeekType type, long move) throws IOException;
		
		/** Returns the new position.
		 * The implementation must not allow to seek after the end of the file, the method setSize should be used instead.
		 * It must accept seeking beyond the end, but position at the end,
		 * and accept seeking before the beginning, but position at the beginning.
		 */
		AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone);

		/** Returns the new position.
		 * The implementation must not allow to seek after the end of the file, the method setSize should be used instead.
		 * It must accept seeking beyond the end, but position at the end,
		 * and accept seeking before the beginning, but position at the beginning.
		 */
		default AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move) { return seekAsync(type, move, null); }
	}

	/** Add the capability to read forward on an IO. */
	public interface Readable extends IO {
		/** Return a synchronization point that is unblocked when data is ready to be read.
		 * This allows to start reading operations only when we know it won't block.
		 */
		IAsync<IOException> canStartReading();
		
		/** Read synchronously into the given buffer.
		 * The buffer may not be filled, but at least one byte is read.
		 * If there is no available byte, this method is blocking.
		 * @param buffer buffer to fill
		 * @return the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		int readSync(ByteBuffer buffer) throws IOException;
		
		/** Asynchronous read operation.
		 * The buffer may not be filled, but at least one byte is read.
		 * If there is no available byte, the returned AsyncWork won't be unblocked until at least one
		 * byte is available, or we know that the end of the IO has been reached.
		 * @param buffer buffer to fill
		 * @param ondone called before the returned AsyncWork is unblocked and its listeners are called.
		 * @return the Integer contains the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		
		AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to readAsync(buffer, null). */
		default AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer) { return readAsync(buffer, null); }
		
		/** Synchronous read operation to fully fill the given buffer.
		 * This method does not return until the buffer has been fully filled, or the end of the IO has been reached.
		 * @param buffer the buffer to fill
		 * @return the number of bytes read, possibly 0 if the end of the IO has been reached.
		 */
		int readFullySync(ByteBuffer buffer) throws IOException;
		
		/** Asynchronous read operation to fully fill the given buffer.
		 * The returned AsyncWork is not unblocked until the buffer has been fully filled, or the end of the IO has been reached.
		 * @param buffer the buffer to fill
		 * @param ondone called before the returned AsyncWork is unblocked and its listeners are called.
		 * @return the Integer contains the number of bytes read, possibly 0 if the end of the IO has been reached.
		 */
		AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to readFullyAsync(buffer, null). */
		default AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer) { return readFullyAsync(buffer, null); }
		
		/** Returns the number of bytes skipped. */
		long skipSync(long n) throws IOException;
		
		/** Returns the number of bytes skipped. */
		AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone);
		
		/** Returns the number of bytes skipped. */
		default AsyncSupplier<Long,IOException> skipAsync(long n) { return skipAsync(n, null); }

		/** Equivalent to createProducer(8192, false). */
		default AsyncProducer<ByteBuffer, IOException> createProducer(boolean closeOnEnd) {
			return createProducer(8192, false, closeOnEnd);
		}
		
		/** Create a producer that read from this IO. */
		default AsyncProducer<ByteBuffer, IOException> createProducer(int bufferSize, boolean readFully, boolean closeOnEnd) {
			ByteArrayCache cache = ByteArrayCache.getInstance();
			return () -> {
				ByteBuffer buffer = ByteBuffer.wrap(cache.get(bufferSize, true));
				AsyncSupplier<Integer, IOException> read = readFully ? readFullyAsync(buffer) : readAsync(buffer);
				AsyncSupplier<ByteBuffer, IOException> production = new AsyncSupplier<>();
				read.onDone(nb -> {
					if (nb.intValue() <= 0) {
						if (closeOnEnd)
							closeAsync();
						production.unblockSuccess(null);
						return;
					}
					production.unblockSuccess((ByteBuffer)buffer.flip());	
				}, production);
				if (closeOnEnd)
					production.onError(error -> closeAsync());
				return production;
			};
		}
		
		/** Add read operations to read at a specific position.
		 * Important note: operations with a given position must not change the cursor position.
		 */
		public interface Seekable extends IO.Readable, IO.Seekable {
			/** Same as {@link IO.Readable#readSync(ByteBuffer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read. */
			int readSync(long pos, ByteBuffer buffer) throws IOException;

			/** Same as {@link IO.Readable#readAsync(ByteBuffer, Consumer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read upon completion. */
			AsyncSupplier<Integer,IOException>
				readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to readAsync(pos, buffer, null). */
			default AsyncSupplier<Integer,IOException> readAsync(long pos, ByteBuffer buffer) { return readAsync(pos, buffer, null); }
			
			/** Same as {@link IO.Readable#readFullySync(ByteBuffer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read. */
			int readFullySync(long pos, ByteBuffer buffer) throws IOException;
			
			/** Same as {@link IO.Readable#readFullyAsync(ByteBuffer, Consumer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read upon completion. */
			AsyncSupplier<Integer,IOException>
				readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to readFullyAsync(pos, buffer, null). */
			default AsyncSupplier<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer)
			{ return readFullyAsync(pos, buffer, null); }
		}

		/** Indicates the IO is buffered, and add operations. */
		public interface Buffered extends Readable, ReadableByteStream {
			/** Retrieve a buffer of bytes in the most efficient way depending on the implementation of Buffered.<br/>
			 * This method is similar to readAsync, it reads some bytes that are immediately available with a minimum of
			 * operations. The problem with readAsync is that we have to give a buffer to fill. If there are more
			 * bytes that can be read, a new call will be needed. If there not enough available bytes, the given buffer
			 * is not fully filled and uses more memory than needed.<br/>
			 * This method is more efficient because it leaves the implementation allocating a buffer with a size that
			 * can be fully filled in a minimum number of operations.<br/>
			 * In addition, using readAsync needs to keep the given buffer in a variable or in the closure,
			 * while using this method the buffer is directly given as the result of the operation.<br/>
			 * The returned buffer is ready to be read (no need to flip), and the number of bytes read can be obtain
			 * using the method remaining of the buffer.<br/>
			 * Usage of the returned ByteBuffer should take care of its read-only attribute. If not read-only,
			 * the buffer may be reused or should be free using ByteArrayCache.
			 */
			AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone);
			
			/** Equivalent to readNextBufferAsync(null). */
			default AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync() { return readNextBufferAsync(null); }
			
			/** Retrieve a buffer of bytes in the most efficient way depending on the implementation of Buffered.<br/>
			 * Its returns the immediately available bytes with a minimum of operations.<br/>
			 * If the end of stream is reached, null is returned.<br/>
			 * Usage of the returned ByteBuffer should take care of its read-only attribute. If not read-only,
			 * the buffer may be reused or should be free using ByteArrayCache.
			 */
			ByteBuffer readNextBuffer() throws IOException;
			
			/** Read a single byte if possible.
			 * @return the next byte, or -1 if the end of the IO has been reached, or -2 if no more byte is available.
			 */
			int readAsync() throws IOException;
			
			/** While readAsync methods are supposed to do the job in a separate thread, this method
			 * fills the given buffer synchronously if enough data is already buffered, else it finishes asynchronously.
			 * The caller can check the returned AsyncWork by calling its method isUnblocked to know if the
			 * read has been performed synchronously.
			 * This method may be useful for processes that hope to work synchronously because this IO is buffered,
			 * but support also to work asynchronously without blocking a thread.
			 */
			AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone);

			/** While readAsync methods are supposed to do the job in a separate thread, this method
			 * fills the given buffer synchronously if enough data is already buffered, else it finishes asynchronously.
			 * The caller can check the returned AsyncWork by calling its method isUnblocked to know if the
			 * read has been performed synchronously.
			 * This method may be useful for processes that hope to work synchronously because this IO is buffered,
			 * but support also to work asynchronously without blocking a thread.
			 */
			default AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer) {
				return readFullySyncIfPossible(buffer, null);
			}
			
			/** Read this IO buffer by buffer (using readNextBufferAsync) and forward each buffer to the given consumer. */
			@Override
			default AsyncProducer<ByteBuffer, IOException> createProducer(boolean closeOnEnd) {
				return () -> {
					AsyncSupplier<ByteBuffer, IOException> read = readNextBufferAsync();
					if (closeOnEnd)
						read.onDone(() -> {
							if (!read.isSuccessful() || read.getResult() == null)
								closeAsync();
						});
					return read;
				};
			}
		}
	}
	
	/** Simple interface for something to which we can write buffers asynchronously. */
	public interface WriterAsync {
		
		/** Write asynchronously all bytes available in the given buffer at the current position.
		 * If not all bytes can be written, an error must be returned.
		 * Return the number of bytes written.
		 */
		AsyncSupplier<Integer,IOException> writeAsync(ByteBuffer buffer);
		
		/** Create an AsyncConsumer that writes to this IO. */
		default AsyncConsumer<ByteBuffer, IOException> createConsumer(Runnable onEnd, Consumer<IOException> onError) {
			return new AsyncConsumer<ByteBuffer, IOException>() {

				@Override
				public IAsync<IOException> consume(ByteBuffer data) {
					AsyncSupplier<Integer, IOException> write = writeAsync(data);
					if (data.hasArray() && !data.isReadOnly())
						write.onDone(() -> ByteArrayCache.getInstance().free(data.array()));
					return write;
				}

				@Override
				public IAsync<IOException> end() {
					onEnd.run();
					return new Async<>(true);
				}

				@Override
				public void error(IOException error) {
					onError.accept(error);
				}
				
			};
		}

	}
	
	/** Add the capability to write forward on an IO. */
	public interface Writable extends IO, WriterAsync {
		/** Return a synchronization point that is unblocked when data is ready to be written.
		 * This allows to start writing operations only when we know it won't block.
		 */
		IAsync<IOException> canStartWriting();

		/** Write synchronously all bytes available in the given buffer at the current position.
		 * If not all bytes can be written, an error must be thrown.
		 * Return the number of bytes written.
		 */
		int writeSync(ByteBuffer buffer) throws IOException;
		
		/** Write asynchronously all bytes available in the given buffer at the current position.
		 * If not all bytes can be written, an error must be returned.
		 * Return the number of bytes written.
		 */
		AsyncSupplier<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to writeAsync(buffer, null). */
		@Override
		default AsyncSupplier<Integer,IOException> writeAsync(ByteBuffer buffer) { return writeAsync(buffer, null); }
		
		/** Add operations to write at a specific position.
		 * Important note: operations with a given position must not change the cursor position.
		 */
		public interface Seekable extends Writable, IO.Seekable {
			
			/** Same as {@link IO.Writable#writeSync(ByteBuffer)} but at the given position.
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			int writeSync(long pos, ByteBuffer buffer) throws IOException;
			
			/** Same as {@link IO.Writable#writeAsync(ByteBuffer, Consumer)} but at the given position.
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			AsyncSupplier<Integer,IOException>
				writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to writeAsync(pos, buffer, null).
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			default AsyncSupplier<Integer,IOException> writeAsync(long pos, ByteBuffer buffer)
			{ return writeAsync(pos, buffer, null); }
		}
		
		/** Add a flush operation to force writing on the underlying IO. */
		public interface Buffered extends Writable, WritableByteStream {
			/** Force to write buffered data to the underlying IO.
			 * The operation is asynchronous, and return a synchronization point.
			 */
			IAsync<IOException> flush();
		}		
	}
	
	
	/** Add capability to know the size of an IO. */
	public interface KnownSize extends IO {
		/** Synchronous operation to get the size of the IO. */
		long getSizeSync() throws IOException;

		/** Asynchronous operation to get the size of the IO. */
		AsyncSupplier<Long, IOException> getSizeAsync();
	}

	
	/** Add capability to resize the IO. */
	public interface Resizable extends IO.KnownSize {
		/** Synchronous resize. */
		void setSizeSync(long newSize) throws IOException;
		
		/** Asynchronous resize. */
		IAsync<IOException> setSizeAsync(long newSize);
	}
	
	
	/** Convert an Exception into an IOException. If thie given exception is already an IOException, it is directly returned. */
	static IOException error(Throwable e) {
		if (e instanceof IOException) return (IOException)e;
		return new IOException(e);
	}

	/** Create an IOException from a CancelException. */
	static IOException errorCancelled(CancelException e) {
		return new IOException("Cancelled", e);
	}

	/** Return a CancelException with message IO closed. */
	static CancelException cancelClosed() {
		return new CancelException("IO closed");
	}
	
	/** Add capability to read byte by byte, typically a buffered IO. */
	public interface ReadableByteStream extends IO {
		
		/** Return a synchronization point that is unblocked when data is ready to be read.
		 * This allows to start reading operations only when we know it won't block.
		 */
		IAsync<IOException> canStartReading();

		/** Read next byte.
		 * Return -1 if the end of the IO is reached, or the value between 0 and 255.
		 */
		int read() throws IOException;
		
		/** Read bytes into the given buffer, starting to store them at the given offset, and with the given maximum number of bytes.
		 * Returns the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		int read(byte[] buffer, int offset, int len) throws IOException;
		
		/** Read bytes to fill the given buffer, and return the number of bytes read which is less than the
		 * size of the buffer only if the end of the IO is reached, and so may return 0.
		 */
		int readFully(byte[] buffer) throws IOException;
		
		/** Similar to read, but returns a byte and throw an EOFException in case the end is reached. */
		default byte readByte() throws IOException {
			int b = read();
			if (b < 0) throw new EOFException();
			return (byte)b;
		}
		
		/** Same as {@link IO.Readable#skipSync(long)} but limited to an integer. */
		int skip(int skip) throws IOException;
	}
	
	/** Add capability to write byte by byte. */
	public interface WritableByteStream extends IO {
		/** Write a byte. */
		void write(byte b) throws IOException;
		
		/** Write bytes from the given buffer. */
		void write(byte[] buffer, int offset, int length) throws IOException;
		
		/** Write bytes from the given buffer. */
		default void write(byte[] buffer) throws IOException {
			write(buffer, 0, buffer.length);
		}

		/** Return a synchronization point that is unblocked when data is ready to be written.
		 * This allows to start writing operations only when we know it won't block.
		 */
		IAsync<IOException> canStartWriting();
	}
	
	/** An OutputToInput is an IO on which a producer is writing, and a consumer is reading.<br/>
	 * The producer receive it as a Writable, and the consumer as a Readable.<br/>
	 * Both can work concurrently: as soon as data is written by the producer, it can be read by the consumer.<br/>
	 * The producer must call the method endOfData or signalErrorBeforeEndOfData, so the consumer can
	 * receive the information it reaches the end of the IO.<br/>
	 * The implementations may add additional capabilities such as Readable.Seekable if it keeps the data.
	 */
	public interface OutputToInput extends IO.Readable, IO.Writable {
		/** Signal that no more data will be written by the producer. */
		void endOfData();
		
		/** Signal that no more data will be written by the producer because of an error. */
		void signalErrorBeforeEndOfData(IOException error);
		
		/** Return true if endOfData has been already called. */ 
		boolean isFullDataAvailable();
		
		/** If available, return the size. A negative value means no size available. */
		long getAvailableDataSize();
		
		/** Create an AsyncConsumer that writes to this OutputToInput. */
		default AsyncConsumer<ByteBuffer, IOException> createConsumer() {
			return createConsumer(this::endOfData, this::signalErrorBeforeEndOfData);
		}

	}
	
}
