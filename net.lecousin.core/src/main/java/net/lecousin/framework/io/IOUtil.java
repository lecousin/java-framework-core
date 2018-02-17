package net.lecousin.framework.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListenerReady;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Utility methods for IO.
 */
public final class IOUtil {
	
	private IOUtil() { /* no instance */ }

	/**
	 * Fill the remaining bytes of the given buffer.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @return the number of bytes read, which is less than the remaining bytes of the buffer only if the end of the IO is reached.
	 * @throws IOException in case of error reading bytes
	 */
	public static int readFully(IO.Readable io, ByteBuffer buffer) throws IOException {
		int read = 0;
		while (buffer.hasRemaining()) {
			int nb = io.readSync(buffer);
			if (nb <= 0) break;
			read += nb;
		}
		return read;
	}
	
	/**
	 * Fill the given buffer.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @return the number of bytes read, which is less than the length of the buffer only if the end of the IO is reached.
	 * @throws IOException in case of error reading bytes
	 */
	public static int readFully(IO.ReadableByteStream io, byte[] buffer) throws IOException {
		return readFully(io, buffer, 0, buffer.length);
	}
	
	/**
	 * Read the given number of bytes.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param off the offset of the buffer from which to fill
	 * @param len the number of bytes to read
	 * @return the number of bytes read, which is less than the len only if the end of the IO is reached.
	 * @throws IOException in case of error reading bytes
	 */
	public static int readFully(IO.ReadableByteStream io, byte[] buffer, int off, int len) throws IOException {
		int read = 0;
		while (read < len) {
			int nb = io.read(buffer, off + read, len - read);
			if (nb <= 0) break;
			read += nb;
		}
		return read;
	}
	
	/**
	 * Fill the remaining bytes of the given buffer.
	 * @param io the readable to read from
	 * @param pos the position in the io to start from
	 * @param buffer the buffer to fill
	 * @return the number of bytes read, which is less than the remaining bytes of the buffer only if the end of the IO is reached.
	 * @throws IOException in case of error reading bytes
	 */
	public static int readFullySync(IO.Readable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		int read = 0;
		while (buffer.hasRemaining()) {
			int nb = io.readSync(pos, buffer);
			if (nb <= 0) break;
			read += nb;
			pos += nb;
		}
		return read;
	}
	
	/**
	 * Fill the remaining bytes of the given buffer asynchronously.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read, which is less than the remaining bytes of the buffer only if the end of the IO is reached.
	 */
	public static AsyncWork<Integer,IOException> readFullyAsync(
		IO.Readable io, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		return readFullyAsync(io,buffer,0,ondone);
	}
	
	/**
	 * Fill the remaining bytes of the given buffer asynchronously.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param done number of bytes already read (will be added to the returned number of bytes read)
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read, which is less than the remaining bytes of the buffer only if the end of the IO is reached.
	 */
	public static AsyncWork<Integer,IOException> readFullyAsync(
		IO.Readable io, ByteBuffer buffer, int done, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		AsyncWork<Integer,IOException> read = io.readAsync(buffer);
		if (read.isUnblocked()) {
			if (!read.isSuccessful()) {
				if (ondone != null && read.getError() != null) ondone.run(new Pair<>(null, read.getError()));
				return read;
			}
			if (!buffer.hasRemaining()) {
				if (done == 0) {
					if (ondone != null) ondone.run(new Pair<>(read.getResult(), null));
					return read;
				}
				if (read.getResult().intValue() <= 0) {
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(done), null));
					return new AsyncWork<>(Integer.valueOf(done), null);
				}
				if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(read.getResult().intValue() + done),null));
				return new AsyncWork<>(Integer.valueOf(read.getResult().intValue() + done),null);
			}
			if (read.getResult().intValue() <= 0) {
				if (done == 0) {
					if (ondone != null) ondone.run(new Pair<>(read.getResult(), null));
					return read;
				}
				if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(done), null));
				return new AsyncWork<>(Integer.valueOf(done), null);
			}
			return readFullyAsync(io, buffer, read.getResult().intValue() + done, ondone);
		}
		AsyncWork<Integer,IOException> sp = new AsyncWork<>();
		MutableInteger total = new MutableInteger(done);
		Mutable<AsyncWork<Integer,IOException>> r = new Mutable<>(read);
		read.listenInline(new AsyncWorkListenerReady<Integer, IOException>((result, that) -> {
			do {
				if (!buffer.hasRemaining() || result.intValue() <= 0) {
					if (total.get() == 0) {
						if (ondone != null) ondone.run(new Pair<>(result, null));
						sp.unblockSuccess(result);
					} else if (result.intValue() >= 0) {
						Integer i = Integer.valueOf(result.intValue() + total.get());
						if (ondone != null) ondone.run(new Pair<>(i, null));
						sp.unblockSuccess(i);
					} else {
						if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(total.get()), null));
						sp.unblockSuccess(Integer.valueOf(total.get()));
					}
					return;
				}
				total.add(result.intValue());
				AsyncWork<Integer, IOException> reading = io.readAsync(buffer);
				r.set(reading);
				if (reading.isSuccessful()) result = reading.getResult();
				else break;
			} while (true);
			r.get().listenInline(that);
		}, sp, ondone));
		sp.onCancel(new Listener<CancelException>() {
			@Override
			public void fire(CancelException event) {
				r.get().unblockCancel(event);
			}
		});
		return sp;
	}
	
	/**
	 * Fill the remaining bytes of the given buffer.
	 * @param io the readable to read from
	 * @param pos the position in the io to start from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read, which is less than the remaining bytes of the buffer only if the end of the IO is reached.
	 */
	public static AsyncWork<Integer,IOException> readFullyAsync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		AsyncWork<Integer,IOException> read = io.readAsync(pos, buffer);
		if (read.isUnblocked()) {
			if (ondone != null) {
				if (read.getError() != null) ondone.run(new Pair<>(null, read.getError()));
				else if (read.getResult() != null) ondone.run(new Pair<>(read.getResult(), null));
			}
			if (!read.isSuccessful()) return read;
			if (!buffer.hasRemaining()) return read;
			if (read.getResult().intValue() <= 0) return read;
		}
		AsyncWork<Integer,IOException> sp = new AsyncWork<>();
		MutableInteger total = new MutableInteger(0);
		Mutable<AsyncWork<Integer,IOException>> r = new Mutable<>(read);
		read.listenInline(new AsyncWorkListenerReady<Integer, IOException>((result, that) -> {
			do {
				if (!buffer.hasRemaining() || result.intValue() <= 0) {
					if (total.get() == 0) {
						if (ondone != null) ondone.run(new Pair<>(result, null));
						sp.unblockSuccess(result);
					} else if (result.intValue() >= 0) {
						Integer i = Integer.valueOf(result.intValue() + total.get());
						if (ondone != null) ondone.run(new Pair<>(i, null));
						sp.unblockSuccess(i);
					} else {
						Integer i = Integer.valueOf(total.get());
						if (ondone != null) ondone.run(new Pair<>(i, null));
						sp.unblockSuccess(i);
					}
					return;
				}
				total.add(result.intValue());
				AsyncWork<Integer, IOException> reading = io.readAsync(pos + total.get(), buffer);
				r.set(reading);
				if (reading.isSuccessful()) result = reading.getResult();
				else break;
			} while (true);
			r.get().listenInline(that);
		}, sp, ondone));
		sp.onCancel(new Listener<CancelException>() {
			@Override
			public void fire(CancelException event) {
				r.get().unblockCancel(event);
			}
		});
		return sp;
	}
	
	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static Task<Integer,IOException> readAsyncUsingSync(
		IO.Readable io, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer,IOException>(
			"Reading from " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.readSync(buffer));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param pos the position in the io to start from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static Task<Integer,IOException> readAsyncUsingSync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer,IOException>(
			"Reading from " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.readSync(pos, buffer));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static Task<Integer,IOException> readFullyAsyncUsingSync(
		IO.Readable io, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer,IOException>(
			"Reading from " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.readFullySync(buffer));
			}
		};
		task.start();
		return task;
	}

	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param pos the position in the io to start from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static Task<Integer,IOException> readFullyAsyncUsingSync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer,IOException>(
			"Reading from " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.readFullySync(pos, buffer));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Implement a synchronous skip using a synchronous read.
	 */
	public static long skipSyncByReading(IO.Readable io, long n) throws IOException {
		if (n <= 0) return 0;
		int l = n > 65536 ? 65536 : (int)n;
		ByteBuffer b = ByteBuffer.allocate(l);
		long total = 0;
		while (total < n) {
			int len = n - total > l ? l : (int)(n - total);
			b.clear();
			b.limit(len);
			int nb = io.readSync(b);
			if (nb <= 0) break;
			total += nb;
		}
		return total;
	}
	
	/**
	 * Implement an asynchronous skip using a task calling a synchronous skip.
	 * This must be used only if the synchronous skip is using only CPU.
	 */
	public static AsyncWork<Long,IOException> skipAsyncUsingSync(IO.Readable io, long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Skipping bytes", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException {
				long total = skipSyncByReading(io, n);
				return Long.valueOf(total);
			}
		};
		task.start();
		return task.getOutput();
	}
	
	/**
	 * Implement an asynchronous skip using readAsync.
	 */
	public static AsyncWork<Long, IOException> skipAsyncByReading(IO.Readable io, long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		if (n <= 0) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			return new AsyncWork<>(Long.valueOf(0), null);
		}
		ByteBuffer b = ByteBuffer.allocate(n > 65536 ? 65536 : (int)n);
		MutableLong done = new MutableLong(0);
		AsyncWork<Long, IOException> result = new AsyncWork<>();
		io.readAsync(b).listenInline(new AsyncWorkListenerReady<Integer, IOException>((nb, that) -> {
			AsyncWork<Integer, IOException> next;
			do {
				int read = nb.intValue();
				if (read <= 0) {
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done.get()), null));
					result.unblockSuccess(Long.valueOf(done.get()));
					return;
				}
				done.add(nb.intValue());
				if (done.get() == n) {
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(n), null));
					result.unblockSuccess(Long.valueOf(n));
					return;
				}
				b.clear();
				if (n - done.get() < b.remaining())
					b.limit((int)(n - done.get()));
				next = io.readAsync(b);
				if (next.isSuccessful()) nb = next.getResult();
				else break;
			} while (true);
			next.listenInline(that);
		}, result));
		return result;
	}
	
	/**
	 * Write the given Readable to a temporary file, and return the temporary file.
	 */
	public static File toTempFile(IO.Readable io) throws IOException {
		File file = File.createTempFile("net.lecousin.framework.io", "streamtofile");
		ByteBuffer buffer = ByteBuffer.allocateDirect(65536);
		try (RandomAccessFile f = new RandomAccessFile(file, "w"); FileChannel c = f.getChannel()) {
			do {
				int nb = io.readFullySync(buffer);
				if (nb <= 0) break;
				buffer.flip();
				c.write(buffer);
				buffer.clear();
			} while (true);
			return file;
		}
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readSyncUsingAsync(IO.Readable io, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.readAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readSyncUsingAsync(IO.Readable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.readAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readFullySyncUsingAsync(IO.Readable io, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.readFullyAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readFullySyncUsingAsync(IO.Readable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.readFullyAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous skip using an asynchronous one and blocking until it finishes.
	 */
	public static long skipSyncUsingAsync(IO.Readable io, long n) throws IOException {
		AsyncWork<Long,IOException> sp = io.skipAsync(n);
		try { return sp.blockResult(0).longValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous write using an asynchronous one and blocking until it finishes.
	 */
	public static int writeSyncUsingAsync(IO.Writable io, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.writeAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement a synchronous write using an asynchronous one and blocking until it finishes.
	 */
	public static int writeSyncUsingAsync(IO.Writable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncWork<Integer,IOException> sp = io.writeAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw new IOException("Cancelled",e); }
	}
	
	/**
	 * Implement an asynchronous write using a task calling a synchronous write.
	 * This must be used only if the synchronous write is only using CPU.
	 */
	public static Task<Integer,IOException> writeAsyncUsingSync(
		IO.Writable io, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer, IOException>(
			"Writing to " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.writeSync(buffer));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Implement an asynchronous write using a task calling a synchronous write.
	 * This must be used only if the synchronous write is only using CPU.
	 */
	public static Task<Integer,IOException> writeAsyncUsingSync(
		IO.Writable.Seekable io, long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Integer,IOException> task = new Task.Cpu<Integer, IOException>(
			"Writing to " + io.getSourceDescription(), io.getPriority(), ondone
		) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(io.writeSync(pos, buffer));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Read all bytes from the given Readable and convert it as a String using the given charset encoding.
	 */
	@SuppressWarnings("resource")
	public static Task<UnprotectedStringBuffer,IOException> readFullyAsString(IO.Readable io, Charset charset, byte priority) {
		IO.Readable.Buffered bio;
		if (io instanceof IO.Readable.Buffered)
			bio = (IO.Readable.Buffered)io;
		else
			bio = new PreBufferedReadable(io, 1024, priority, 8192, priority, 8);
		Task<UnprotectedStringBuffer,IOException> task = new Task.Cpu<UnprotectedStringBuffer,IOException>(
			"Read file as string: " + io.getSourceDescription(), priority
		) {
			@Override
			public UnprotectedStringBuffer run() throws IOException {
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(bio, charset, 64, 128);
				UnprotectedStringBuffer str = new UnprotectedStringBuffer();
				do {
					try { str.append(stream.read()); }
					catch (EOFException e) { break; }
				} while (true);
				return str;
			}
		};
		task.startOn(bio.canStartReading(), false);
		return task;
	}
	
	/**
	 * Read all bytes from the given Readable and append them to the given StringBuilder using the given charset encoding.
	 */
	public static void readFullyAsStringSync(IO.Readable io, Charset charset, StringBuilder s) throws IOException {
		byte[] buf = new byte[1024];
		do {
			int nb = io.readFullySync(ByteBuffer.wrap(buf));
			if (nb <= 0) break;
			s.append(new String(buf, 0, nb, charset));
			if (nb < 1024) break;
		} while (true);
	}

	/**
	 * Read all bytes from the given Readable and append them to the given StringBuilder using the given charset encoding.
	 */
	public static void readFullyAsStringSync(InputStream input, Charset charset, StringBuilder s) throws IOException {
		byte[] buf = new byte[1024];
		do {
			int nb = input.read(buf);
			if (nb <= 0) break;
			s.append(new String(buf, 0, nb, charset));
			if (nb < 1024) break;
		} while (true);
	}

	/**
	 * Read all bytes from the given Readable and convert it as a String using the given charset encoding.
	 */
	public static String readFullyAsStringSync(IO.Readable io, Charset charset) throws IOException {
		if (io instanceof IO.KnownSize) {
			byte[] bytes = new byte[(int)((IO.KnownSize)io).getSizeSync()];
			io.readFullySync(ByteBuffer.wrap(bytes));
			return new String(bytes, charset);
		}
		StringBuilder s = new StringBuilder(1024);
		readFullyAsStringSync(io, charset, s);
		return s.toString();
	}
	
	/**
	 * Read all bytes from the given file and convert it as a String using the given charset encoding.
	 */
	public static String readFullyAsStringSync(File file, Charset charset) throws IOException {
		try (FileIO.ReadOnly io = new FileIO.ReadOnly(file, Task.PRIORITY_RATHER_IMPORTANT)) {
			return readFullyAsStringSync(io, charset);
		} catch (Exception e) {
			throw IO.error(e);
		}
	}
	
	/**
	 * Read all bytes from the given InputStream and convert it as a String using the given charset encoding.
	 */
	public static String readFullyAsStringSync(InputStream input, Charset charset) throws IOException {
		StringBuilder s = new StringBuilder(1024);
		readFullyAsStringSync(input, charset, s);
		return s.toString();
	}
	
	/**
	 * Implement an asynchronous seek using a task calling a synchronous one.
	 * This must be used only if the synchronous seek is only using CPU.
	 */
	public static Task<Long,IOException> seekAsyncUsingSync(
		IO.Readable.Seekable io, SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone
	) {
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Seeking", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException {
				return Long.valueOf(io.seekSync(type, move));
			}
		};
		task.start();
		return task;
	}
	
	/**
	 * Implement a synchronous sync by calling an asynchronous one and blocking until the result is available.
	 */
	public static long seekSyncUsingAsync(IO.Readable.Seekable io, SeekType type, long move) throws IOException {
		AsyncWork<Long,IOException> seek = io.seekAsync(type, move);
		seek.blockException(0);
		return seek.getResult().longValue();
	}
	
	/**
	 * Implement an asynchronous setSize using a task calling a synchronous one.
	 * This must be used only if the synchronous setSize is only using CPU.
	 */
	public static Task<Void,IOException> setSizeAsyncUsingSync(IO.Resizable io, long newSize,byte priority) {
		Task<Void,IOException> task = new Task.Cpu<Void,IOException>("Resizing " + io.getSourceDescription(), priority) {
			@Override
			public Void run() throws IOException {
				io.setSizeSync(newSize);
				return null;
			}
		};
		task.start();
		return task;
	}


	// ---- copy input to output ----
	
	/**
	 * Copy from a Readable to a Writable.
	 * @param input input to read from
	 * @param output output to write to
	 * @param size number of bytes to copy, or -1 if it is not known
	 * @param closeIOs if true both IOs will be closed at the end of the copy
	 * @param progress progress
	 * @param work amount of work in the progress
	 * @return number of bytes copied
	 */
	public static AsyncWork<Long, IOException> copy(
		IO.Readable input, IO.Writable output, long size, boolean closeIOs, WorkProgress progress, long work
	) {
		AsyncWork<Long, IOException> sp = new AsyncWork<>();
		copy(input, output, size, closeIOs, sp, progress, work);
		return sp;
	}
	
	@SuppressWarnings("resource")
	private static void copy(
		IO.Readable input, IO.Writable output, long size, boolean closeIOs, AsyncWork<Long, IOException> sp, WorkProgress progress, long work
	) {
		if (size == 0) {
			if (progress != null) progress.progress(work);
			copyEnd(input, output, sp, null, null, closeIOs, 0);
			return;
		}
		if (size < 0) {
			if (input instanceof IO.KnownSize) {
				AsyncWork<Long, IOException> getSize = ((IO.KnownSize)input).getSizeAsync();
				getSize.listenInline(new Runnable() {
					@Override
					public void run() {
						if (getSize.hasError())
							copyEnd(input, output, sp, getSize.getError(), null, closeIOs, 0);
						else if (getSize.isCancelled())
							copyEnd(input, output, sp, null, getSize.getCancelEvent(), closeIOs, 0);
						else
							copy(input, output, getSize.getResult().longValue(), closeIOs, sp, progress, work);
					}
				});
				return;
			}
			// unknown size
			TaskManager tmIn = getUnderlyingTaskManager(input);
			TaskManager tmOut = getUnderlyingTaskManager(output);
			if (tmIn == tmOut) {
				// same task manager
				//  - no need to pre-fill many buffers in input
				//  - use a buffer size quite large
				copySameTM(input, output, 2 * 1024 * 1024, -1, sp, closeIOs, progress, work);
				return;
			}
			// different task manager
			if (input instanceof IO.Readable.Buffered) {
				// input is already buffered, let's use it as is
				copyStep((IO.Readable.Buffered)input, output, sp, 0, closeIOs, progress, work);
				return;
			}
			PreBufferedReadable binput = new PreBufferedReadable(input, 65536, input.getPriority(), 1024 * 1024, input.getPriority(), 16);
			copyStep(binput, output, sp, 0, closeIOs, progress, work);
			return;
		}
		// known size
		if (size <= 256 * 1024) {
			// better to first read fully, then write fully
			copySameTM(input, output, (int)size, size, sp, closeIOs, progress, work);
			return;
		}
		TaskManager tmIn = getUnderlyingTaskManager(input);
		TaskManager tmOut = getUnderlyingTaskManager(output);
		if (tmIn == tmOut) {
			// same task manager
			if (size <= 4 * 1024 * 1024) {
				// less then 4MB, better to first read fully, then write fully
				copySameTM(input, output, (int)size, size, sp, closeIOs, progress, work);
				return;
			}
			if (size <= 8 * 1024 * 1024) {
				// let's do it in 2 buffers
				copySameTM(input, output, (int)(size / 2 + 1), size, sp, closeIOs, progress, work);
				return;
			}
			copySameTM(input, output, 4 * 1024 * 1024, size, sp, closeIOs, progress, work);
			return;
		}
		// different task manager
		if (input instanceof IO.Readable.Buffered) {
			// input is already buffered, let's use it as is
			copyStep((IO.Readable.Buffered)input, output, sp, 0, closeIOs, progress, work);
			return;
		}
		PreBufferedReadable binput;
		if (size <= 1024 * 1024)
			binput = new PreBufferedReadable(input, size, 64 * 1024, input.getPriority(), 128 * 1024, input.getPriority(), 6);
		else if (size <= 16 * 1024 * 1024)
			binput = new PreBufferedReadable(input, size, 128 * 1024, input.getPriority(), 512 * 1024, input.getPriority(), 8);
		else
			binput = new PreBufferedReadable(input, size, 256 * 1024, input.getPriority(), 2 * 1024 * 1024, input.getPriority(), 5);
		copyStep(binput, output, sp, 0, closeIOs, progress, work);
	}
	
	private static void copyEnd(
		IO.Readable input, IO.Writable output,
		AsyncWork<Long, IOException> sp, IOException error, CancelException cancel,
		boolean closeIOs, long written
	) {
		if (!closeIOs) {
			if (error != null)
				sp.error(error);
			else if (cancel != null)
				sp.cancel(cancel);
			else
				sp.unblockSuccess(Long.valueOf(written));
			return;
		}
		ISynchronizationPoint<Exception> sp1 = input.closeAsync();
		ISynchronizationPoint<Exception> sp2 = output.closeAsync();
		JoinPoint.fromSynchronizationPoints(sp1, sp2).listenInline(new Runnable() {
			@Override
			public void run() {
				Exception e = error;
				if (e == null) {
					if (sp1.hasError()) e = sp1.getError();
					else if (sp2.hasError()) e = sp2.getError();
				}
				if (e != null) {
					sp.error(IO.error(e));
					return;
				}
				if (cancel != null)
					sp.cancel(cancel);
				else
					sp.unblockSuccess(Long.valueOf(written));
			}
		});
	}

	
	private static void copySameTM(
		IO.Readable input, IO.Writable output, int bufferSize, long total, 
		AsyncWork<Long,IOException> end, boolean closeIOs, WorkProgress progress, long work
	) {
		new Task.Cpu<Void, NoException>("Allocate buffers to copy IOs", input.getPriority()) {
			@Override
			public Void run() {
				copySameTMStep(input, output, ByteBuffer.allocate(bufferSize), 0, total, end, closeIOs, progress, work);
				return null;
			}
		}.start();
	}
	
	private static void copySameTMStep(
		IO.Readable input, IO.Writable output, ByteBuffer buf, long written, long total,
		AsyncWork<Long,IOException> end, boolean closeIOs, WorkProgress progress, long work
	) {
		input.readFullyAsync(buf).listenInline(new AsyncWorkListener<Integer, IOException>() {
			@Override
			public void ready(Integer result) {
				int nb = result.intValue();
				if (nb <= 0) {
					if (progress != null) progress.progress(work);
					copyEnd(input, output, end, null, null, closeIOs, written);
				} else {
					buf.flip();
					if (progress != null) progress.progress(nb * work / (total * 2));
					AsyncWork<Integer,IOException> write = output.writeAsync(buf);
					write.listenInline(new AsyncWorkListener<Integer, IOException>() {
						@Override
						public void ready(Integer result) {
							long w;
							if (progress != null) {
								w = nb * work / total;
								progress.progress(w - (nb * work / (total * 2)));
								w = work - w;
							} else w = 0;
							if (nb < buf.capacity() || (total > 0 && total == written + result.longValue()))
								copyEnd(input, output, end, null, null, closeIOs, written + result.longValue());
							else {
								buf.clear();
								copySameTMStep(input, output, buf, written + result.longValue(), total,
										end, closeIOs, progress, w);
							}
						}
						
						@Override
						public void error(IOException error) {
							copyEnd(input, output, end, error, null, closeIOs, written);
						}
						
						@Override
						public void cancelled(CancelException event) {
							copyEnd(input, output, end, null, event, closeIOs, written);
						}
					});
				}
			}
			
			@Override
			public void error(IOException error) {
				copyEnd(input, output, end, error, null, closeIOs, written);
			}
			
			@Override
			public void cancelled(CancelException event) {
				copyEnd(input, output, end, null, event, closeIOs, written);
			}
		});
	}

	private static void copyStep(
		IO.Readable.Buffered input, IO.Writable output, AsyncWork<Long, IOException> sp, long written,
		boolean closeIOs, WorkProgress progress, long work
	) {
		AsyncWork<ByteBuffer, IOException> read = input.readNextBufferAsync();
		read.listenInline(new Runnable() {
			@Override
			public void run() {
				if (read.hasError()) {
					copyEnd(input, output, sp, read.getError(), null, closeIOs, written);
					return;
				}
				if (read.isCancelled()) {
					copyEnd(input, output, sp, null, read.getCancelEvent(), closeIOs, written);
					return;
				}
				ByteBuffer buf = read.getResult();
				if (buf == null) {
					if (progress != null) progress.progress(work);
					copyEnd(input, output, sp, null, null, closeIOs, written);
					return;
				}
				if (progress != null && work >= 2) progress.progress(1);
				AsyncWork<Integer, IOException> write = output.writeAsync(buf);
				write.listenInline(new Runnable() {
					@Override
					public void run() {
						if (write.hasError()) {
							copyEnd(input, output, sp, write.getError(), null, closeIOs, written);
							return;
						}
						if (write.isCancelled()) {
							copyEnd(input, output, sp, null, write.getCancelEvent(), closeIOs, written);
							return;
						}
						if (progress != null && work >= 1) progress.progress(1);
						copyStep(input, output, sp, written + write.getResult().intValue(), closeIOs, progress, work - 2);
					}
				});
			}
		});
	}
	
	
	// ---- copy inside the same IO ----
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	/**
	 * Copy bytes inside a Seekable IO.
	 * @param io the IO
	 * @param src source position
	 * @param dst target position
	 * @param len number of bytes to copy
	 */
	public static <T extends IO.Writable.Seekable & IO.Readable.Seekable>
	SynchronizationPoint<IOException> copy(T io, long src, long dst, long len) {
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		if (len < 4 * 1024 * 1024) {
			ByteBuffer buffer = ByteBuffer.allocate((int)len);
			copy(io, src, dst, buffer, len, sp);
		} else {
			ByteBuffer buffer = ByteBuffer.allocate(4 * 1024 * 1024);
			copy(io, src, dst, buffer, len, sp);
		}
		return sp;
	}
	
	private static <T extends IO.Writable.Seekable & IO.Readable.Seekable>
	void copy(T io, long src, long dst, ByteBuffer buffer, long len, SynchronizationPoint<IOException> sp) {
		io.readFullyAsync(src, buffer).listenInline(new Runnable() {
			@Override
			public void run() {
				buffer.flip();
				AsyncWork<Integer, IOException> write = io.writeAsync(dst, buffer);
				if (len <= 4 * 1024 * 1024)
					write.listenInline(sp);
				else write.listenInline(new Runnable() {
					@Override
					public void run() {
						long nl = len - 4 * 1024 * 1024;
						buffer.clear();
						if (nl < 4 * 1024 * 1024) buffer.limit((int)nl);
						copy(io, src + 4 * 1024 * 1024, dst + 4 * 1024 * 1024, buffer, nl, sp);
					}
				});
			}
		});
	}

	
	// ---- copy files ----
	
	/** Copy a file. */
	@SuppressWarnings("resource")
	public static AsyncWork<Long, IOException> copy(
		File src, File dst, byte priority, long knownSize, WorkProgress progress, long work, ISynchronizationPoint<?> startOn
	) {
		AsyncWork<Long, IOException> sp = new AsyncWork<>();
		Task.Cpu<Void,NoException> task = new Task.Cpu<Void,NoException>("Start copying files", priority) {
			@Override
			public Void run() {
				FileIO.ReadOnly input = new FileIO.ReadOnly(src, priority);
				FileIO.WriteOnly output = new FileIO.WriteOnly(dst, priority);
				input.canStart().listenInline(new Runnable() {
					@Override
					public void run() {
						if (input.canStart().hasError()) {
							copyEnd(input, output, sp,
								new IOException("Unable to open file " + src.getAbsolutePath(),
									input.canStart().getError()),
								null, true, 0);
							return;
						}
						copy(input, output, knownSize, true, sp, progress, work);
					}
				});
				return null;
			}
		};
		if (startOn == null)
			task.start();
		else
			task.startOn(startOn, true);
		return sp;
	}

	

	/** Implement an asynchronous close using a task calling the synchronous close. */
	public static ISynchronizationPoint<IOException> closeAsync(Closeable toClose) {
		Task<Void,IOException> task = new Task.Cpu<Void, IOException>("Closing resource", Task.PRIORITY_RATHER_IMPORTANT) {
			@Override
			public Void run() throws IOException {
				toClose.close();
				return null;
			}
		};
		task.start();
		return task.getOutput();
	}
	
	/** Read the content of a file and return a byte array. */
	@SuppressWarnings("resource")
	public static AsyncWork<byte[], IOException> readFully(File file, byte priority) {
		AsyncWork<byte[], IOException> result = new AsyncWork<>();
		FileIO.ReadOnly f = new FileIO.ReadOnly(file, priority);
		f.getSizeAsync().listenInline(new AsyncWorkListener<Long, IOException>() {
			@Override
			public void error(IOException error) {
				try { f.close(); } catch (Throwable e) { /* ignore */ }
				result.error(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				try { f.close(); } catch (Throwable e) { /* ignore */ }
				result.cancel(event);
			}
			
			@Override
			public void ready(Long size) {
				byte[] buf = new byte[size.intValue()];
				f.readFullyAsync(ByteBuffer.wrap(buf)).listenInline(new AsyncWorkListener<Integer, IOException>() {
					@Override
					public void error(IOException error) {
						try { f.close(); } catch (Throwable e) { /* ignore */ }
						result.error(error);
					}
					
					@Override
					public void cancelled(CancelException event) {
						try { f.close(); } catch (Throwable e) { /* ignore */ }
						result.cancel(event);
					}
					
					@Override
					public void ready(Integer read) {
						if (read.intValue() != buf.length)
							result.error(new IOException(
								"Only " + read.intValue() + " bytes read on file size " + buf.length));
						else {
							try { f.close(); } catch (Throwable e) { /* ignore */ }
							result.unblockSuccess(buf);
						}
					}
				});
			}
		});
		return result;
	}
	
	
	/** Get the underlying task manager by going through the wrapped IO. */
	@SuppressWarnings("resource")
	public static TaskManager getUnderlyingTaskManager(IO io) {
		io = getUnderlyingIO(io);
		return io.getTaskManager();
	}
	
	/** Get the underlying IO by going through the wrapped IO until the last one. */
	public static IO getUnderlyingIO(IO io) {
		do {
			@SuppressWarnings("resource")
			IO parent = io.getWrappedIO();
			if (parent == null)
				return io;
			io = parent;
		} while (true);
	}
	
	/** Listen to the given asynchronous operation, then unblock <code>toUnblock</code> with its result/error/cancellation,
	 * but calls ondone before if not null and the result is not cancelled.
	 */
	public static <T> void listenOnDone(
		AsyncWork<T, IOException> toListen, AsyncWork<T, IOException> toUnblock, RunnableWithParameter<Pair<T,IOException>> ondone
	) {
		toListen.listenInline(
			(result) -> {
				if (ondone != null) ondone.run(new Pair<>(result, null));
				toUnblock.unblockSuccess(result);
			},
			(error) -> {
				if (ondone != null) ondone.run(new Pair<>(null, error));
				toUnblock.error(error);
			},
			(cancel) -> {
				toUnblock.cancel(cancel);
			}
		);
	}

	/** Listen to <code>toListen</code>, then calls <code>onReady</code> if it succeed.
	 * In case of error or cancellation, the error or cancellation event is given to <code>onErrorOrCancel</code>.
	 * If ondone is not null, it is called before <code>onReady</code> and <code>onErrorOrCancel</code>.
	 */
	public static <T, T2> void listenOnDone(
		AsyncWork<T, IOException> toListen, Listener<T> onReady, ISynchronizationPoint<IOException> onErrorOrCancel,
		RunnableWithParameter<Pair<T2,IOException>> ondone
	) {
		toListen.listenInline(
			onReady,
			(error) -> {
				if (ondone != null) ondone.run(new Pair<>(null, error));
				onErrorOrCancel.error(error);
			},
			(cancel) -> {
				onErrorOrCancel.cancel(cancel);
			}
		);
	}
	
	/** Listen to <code>toListen</code>, then launch <code>onReady</code> if it succeed.
	 * In case of error or cancellation, the error or cancellation event is given to <code>onErrorOrCancel</code>.
	 * If ondone is not null, it is called before <code>onReady</code> and <code>onErrorOrCancel</code>.
	 */
	public static <T, T2> void listenOnDone(
		AsyncWork<T, IOException> toListen, Task<?,?> onReady, ISynchronizationPoint<IOException> onErrorOrCancel,
		RunnableWithParameter<Pair<T2,IOException>> ondone
	) {
		toListen.listenInline(() -> {
			if (toListen.isCancelled()) {
				if (onErrorOrCancel != null) onErrorOrCancel.cancel(toListen.getCancelEvent());
			} else if (toListen.hasError()) {
				if (ondone != null) ondone.run(new Pair<>(null, toListen.getError()));
				if (onErrorOrCancel != null) onErrorOrCancel.error(toListen.getError());
			} else
				onReady.start();
		});
	}
}
