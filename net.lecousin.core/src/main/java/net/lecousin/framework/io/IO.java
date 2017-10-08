package net.lecousin.framework.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Base interface for all IO, see the package documentation {@link net.lecousin.framework.io}.
 */
public interface IO extends AutoCloseable, Closeable, AsyncCloseable<IOException> {
	
	/** Describe the IO, which can be used for logging or debugging purposes. */
	public String getSourceDescription();

	/** If this IO is wrapping another one (such as adding Buffered capabilities), this method returns it,
	 * or null if it does not wrap another IO.
	 */
	public IO getWrappedIO();
	
	/** Synchronous close. */
	@Override
	public void close() throws IOException;
	
	/** Add a listener to be called once this IO is closed. */
	public void onclose(Runnable listener);
	
	/** Calling this method avoid the IO to be closed. For each call to this function, a call to the unlockClose method must be done. */
	public void lockClose();
	
	/** Allow to close this IO, if this is the last lock on it. If the close method was previously called but locked, the IO is closed. */
	public void unlockClose();
	
	/** Synchronous ou asynchronous operation. */
	public enum OperationType {
		SYNCHRONOUS, ASYNCHRONOUS
	}
	
	/** Return the priority of asynchronous operations. */
	public byte getPriority();

	/** Set the priority of asynchronous operations. */
	public void setPriority(byte priority);
	
	/** Return the TaskManager used for asynchronous operations. */
	public TaskManager getTaskManager();
	
	/** Capability to get the current position on an IO. */
	public interface PositionKnown extends IO {
		/** Return the current position. */
		public long getPosition() throws IOException;
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
		public long seekSync(SeekType type, long move) throws IOException;
		
		/** Returns the new position.
		 * The implementation must not allow to seek after the end of the file, the method setSize should be used instead.
		 * It must accept seeking beyond the end, but position at the end,
		 * and accept seeking before the beginning, but position at the beginning.
		 */
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone);

		/** Returns the new position.
		 * The implementation must not allow to seek after the end of the file, the method setSize should be used instead.
		 * It must accept seeking beyond the end, but position at the end,
		 * and accept seeking before the beginning, but position at the beginning.
		 */
		public default AsyncWork<Long,IOException> seekAsync(SeekType type, long move) { return seekAsync(type, move, null); }
	}

	/** Add the capability to read forward on an IO. */
	public interface Readable extends IO {
		/** Return a synchronization point that is unblocked when data is ready to be read.
		 * This allows to start reading operations only when we know it won't block.
		 */
		public ISynchronizationPoint<IOException> canStartReading();
		
		/** Read synchronously into the given buffer.
		 * The buffer may not be filled, but at least one byte is read.
		 * If there is no available byte, this method is blocking.
		 * @param buffer buffer to fill
		 * @return the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		public int readSync(ByteBuffer buffer) throws IOException;
		
		/** Asynchronous read operation.
		 * The buffer may not be filled, but at least one byte is read.
		 * If there is no available byte, the returned AsyncWork won't be unblocked until at least one
		 * byte is available, or we know that the end of the IO has been reached.
		 * @param buffer buffer to fill
		 * @param ondone called before the returned AsyncWork is unblocked and its listeners are called.
		 * @return the Integer contains the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to readAsync(buffer, null). */
		public default AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer) { return readAsync(buffer, null); }
		
		/** Synchronous read operation to fully fill the given buffer.
		 * This method does not return until the buffer has been fully filled, or the end of the IO has been reached.
		 * @param buffer the buffer to fill
		 * @return the number of bytes read, possibly 0 if the end of the IO has been reached.
		 */
		public int readFullySync(ByteBuffer buffer) throws IOException;
		
		/** Asynchronous read operation to fully fill the given buffer.
		 * The returned AsyncWork is not unblocked until the buffer has been fully filled, or the end of the IO has been reached.
		 * @param buffer the buffer to fill
		 * @param ondone called before the returned AsyncWork is unblocked and its listeners are called.
		 * @return the Integer contains the number of bytes read, possibly 0 if the end of the IO has been reached.
		 */
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to readFullyAsync(buffer, null). */
		public default AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer) { return readFullyAsync(buffer, null); }
		
		/** Returns the number of bytes skipped. */
		public long skipSync(long n) throws IOException;
		
		/** Returns the number of bytes skipped. */
		public AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone);
		
		/** Returns the number of bytes skipped. */
		public default AsyncWork<Long,IOException> skipAsync(long n) { return skipAsync(n, null); }
		
		/** Add read operations to read at a specific position. */
		public interface Seekable extends IO.Readable, IO.Seekable {
			/** Same as {@link IO.Readable#readSync(ByteBuffer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read. */
			public int readSync(long pos, ByteBuffer buffer) throws IOException;

			/** Same as {@link IO.Readable#readAsync(ByteBuffer, RunnableWithParameter)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read upon completion. */
			public AsyncWork<Integer,IOException>
				readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to readAsync(pos, buffer, null). */
			public default AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer) { return readAsync(pos, buffer, null); }
			
			/** Same as {@link IO.Readable#readFullySync(ByteBuffer)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read. */
			public int readFullySync(long pos, ByteBuffer buffer) throws IOException;
			
			/** Same as {@link IO.Readable#readFullyAsync(ByteBuffer, RunnableWithParameter)} but read at the given position.
			 * The current position is changed to the given position plus the number of bytes read upon completion. */
			public AsyncWork<Integer,IOException>
				readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to readFullyAsync(pos, buffer, null). */
			public default AsyncWork<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer)
			{ return readFullyAsync(pos, buffer, null); }
		}

		/** Indicates the IO is buffered, and add operations. */
		public interface Buffered extends Readable, ReadableByteStream {
			/** Return the number of bytes actually buffered and available to read starting from the current position.
			 * This is similar to the {@link java.io.InputStream#available()} method.
			 */
			public int getRemainingBufferedSize();
			
			/** Return the maximum number of bytes buffered by the implementation. */
			public int getMaxBufferedSize();
			
			/** Retrieve a buffer of bytes in the most efficient way depending on the implementation of Buffered.<br/>
			 * This method is similar to readAsync, it read some bytes that are immediately available with a minimum of
			 * operations. The problem with readAsync is that we have to give a buffer to fill. If there are more
			 * bytes that can be read, a new call will be needed. If there not enough available bytes, the given buffer
			 * is not fully filled and uses more memory than needed.<br/>
			 * This method is more efficient because it leaves the implementation allocating a buffer with a size that
			 * can be fully filled in a minimum number of operations.<br/>
			 * In addition, using readAsync needs to keep the given buffer in a variable or in the closure,
			 * while using this method the buffer is directly given as the result of the operation.<br/>
			 * The returned buffer is ready to be read (no need to flip), and the number of bytes read can be obtain
			 * using the method remaining of the buffer.
			 */
			public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone);
			
			/** Equivalent to readNextBufferAsync(null). */
			public default AsyncWork<ByteBuffer, IOException> readNextBufferAsync() { return readNextBufferAsync(null); }
		}
	}
	
	
	/** Add the capability to write forward on an IO. */
	public interface Writable extends IO {
		/** Return a synchronization point that is unblocked when data is ready to be written.
		 * This allows to start writing operations only when we know it won't block.
		 */
		public ISynchronizationPoint<IOException> canStartWriting();

		/** Write synchronously all bytes available in the given buffer at the current position.
		 * If not all bytes can be written, an error must be thrown.
		 * Return the number of bytes written.
		 */
		public int writeSync(ByteBuffer buffer) throws IOException;
		
		/** Write asynchronously all bytes available in the given buffer at the current position.
		 * If not all bytes can be written, an error must be returned.
		 * Return the number of bytes written.
		 */
		public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
		
		/** Equivalent to writeAsync(buffer, null). */
		public default AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer) { return writeAsync(buffer, null); }
		
		/** Add operations to write at a specific position. */
		public interface Seekable extends Writable, IO.Seekable {
			
			/** Same as {@link IO.Writable#writeSync(ByteBuffer)} but at the given position.
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			public int writeSync(long pos, ByteBuffer buffer) throws IOException;
			
			/** Same as {@link IO.Writable#writeAsync(ByteBuffer, RunnableWithParameter)} but at the given position.
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			public AsyncWork<Integer,IOException>
				writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone);
			
			/** Equivalent to writeAsync(pos, buffer, null).
			 * Note that if the IO implements Resizable, the behaviour is different when writing at
			 * the end or beyond the end of the IO: if not resizable, it must throw an exception,
			 * while a resizable IO should accept it except if the resize operation fails.
			 */
			public default AsyncWork<Integer,IOException> writeAsync(long pos, ByteBuffer buffer)
			{ return writeAsync(pos, buffer, null); }
		}
		
		/** Add a flush operation to force writing on the underlying IO. */
		public interface Buffered extends Writable, WritableByteStream {
			/** Force to write buffered data to the underlying IO.
			 * The operation is asynchronous, and return a synchronization point.
			 */
			public ISynchronizationPoint<IOException> flush();
		}		
	}
	
	
	/** Add capability to resize the IO. */
	public interface Resizable extends IO.KnownSize {
		/** Synchronous resize. */
		public void setSizeSync(long newSize) throws IOException;
		
		/** Asynchronous resize. */
		public AsyncWork<Void,IOException> setSizeAsync(long newSize);
	}
	
	
	/** Add capability to know the size of an IO. */
	public interface KnownSize extends IO {
		/** Synchronous operation to get the size of the IO. */
		public long getSizeSync() throws IOException;

		/** Asynchronous operation to get the size of the IO. */
		public AsyncWork<Long, IOException> getSizeAsync();
	}
	
	/** Convert an Exception into an IOException. If thie given exception is already an IOException, it is directly returned. */
	public static IOException error(Exception e) {
		if (e instanceof IOException) return (IOException)e;
		return new IOException(e);
	}
	
	/** Add capability to read byte by byte, typically a buffered IO. */
	public interface ReadableByteStream extends IO {
		
		/** Return a synchronization point that is unblocked when data is ready to be read.
		 * This allows to start reading operations only when we know it won't block.
		 */
		public ISynchronizationPoint<IOException> canStartReading();

		/** Read next byte.
		 * Return -1 if the end of the IO is reached, or the value between 0 and 255.
		 */
		public int read() throws IOException;
		
		/** Read bytes into the given buffer, starting to store them at the given offset, and with the given maximum number of bytes.
		 * Returns the number of bytes read, or 0 or -1 if the end of the IO is reached.
		 */
		public int read(byte[] buffer, int offset, int len) throws IOException;
		
		/** Read bytes to fill the given buffer, and return the number of bytes read which is less than the
		 * size of the buffer only if the end of the IO is reached, and so may return 0.
		 */
		public int readFully(byte[] buffer) throws IOException;
		
		/** Similar to read, but returns a byte and throw an EOFException in case the end is reached. */
		public default byte readByte() throws IOException, EOFException {
			int b = read();
			if (b < 0) throw new EOFException();
			return (byte)b;
		}
		
		/** Same as {@link IO.Readable#skipSync(long)} but limited to an integer. */
		public int skip(int skip) throws IOException;
	}
	
	/** Add capability to write byte by byte. */
	public interface WritableByteStream extends IO {
		/** Write a byte. */
		public void write(byte b) throws IOException;
		
		/** Write bytes from the given buffer. */
		public void write(byte[] buffer, int offset, int length) throws IOException;
		
		/** Write bytes from the given buffer. */
		public default void write(byte[] buffer) throws IOException {
			write(buffer, 0, buffer.length);
		}

		/** Return a synchronization point that is unblocked when data is ready to be written.
		 * This allows to start writing operations only when we know it won't block.
		 */
		public ISynchronizationPoint<IOException> canStartWriting();
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
		public void endOfData();
		
		/** Signal that no more data will be written by the producer because of an error. */
		public void signalErrorBeforeEndOfData(IOException error);
	}
	
	/** Abstract class that implements the methods lockClose, unlockClose and onClose. */
	public abstract static class AbstractIO implements IO {
		private ArrayList<Runnable> closeListeners = null;
		private int closeLocked = 0;
		private SynchronizationPoint<IOException> waitForClose = null;
		
		protected abstract ISynchronizationPoint<IOException> closeIO();
		
		@Override
		public final void close() throws IOException {
			if (closeLocked > 0) return;
			if (closeListeners != null) {
				for (Runnable listener : closeListeners)
					listener.run();
				closeListeners = null;
			}
			ISynchronizationPoint<IOException> sp = closeIO();
			sp.block(0);
			if (sp.hasError()) throw sp.getError();
		}
		
		@Override
		public ISynchronizationPoint<IOException> closeAsync() {
			synchronized (this) {
				if (closeLocked > 0) {
					if (waitForClose == null) waitForClose = new SynchronizationPoint<>();
					return waitForClose;
				}
			}
			if (closeListeners != null) {
				for (Runnable listener : closeListeners)
					listener.run();
				closeListeners = null;
			}
			return closeIO();
		}
		
		@Override
		public void onclose(Runnable listener) {
			synchronized (this) {
				if (closeListeners == null) closeListeners = new ArrayList<>(2);
				closeListeners.add(listener);
			}
		}
		
		@Override
		public synchronized void lockClose() {
			closeLocked++;
		}
		
		@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
		@Override
		public void unlockClose() {
			boolean unblock = false;
			synchronized (this) {
				if (--closeLocked == 0) unblock = waitForClose != null;
			}
			if (unblock) {
				if (closeListeners != null) {
					for (Runnable listener : closeListeners)
						listener.run();
					closeListeners = null;
				}
				closeIO().listenInline(waitForClose);
				waitForClose = null;
			}
		}
	}
	
}
