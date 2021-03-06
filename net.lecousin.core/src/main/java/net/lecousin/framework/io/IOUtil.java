package net.lecousin.framework.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.AsyncSupplier.Listener;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.ByteBuffersIO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.text.CharArrayStringBuffer;
import net.lecousin.framework.util.Pair;

/**
 * Utility methods for IO.
 */
public final class IOUtil {
	
	private IOUtil() {
		/* no instance */
	}

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
	
	/** Read fully into a byte[], and unblock the given result upon completion. */
	@SuppressWarnings("java:S1905") // java needs to specify both types in the cast
	public static void readFully(IO.Readable io, AsyncSupplier<byte[], IOException> result) {
		if (io instanceof IO.KnownSize) {
			readFullyKnownSize((IO.Readable & IO.KnownSize)io, result);
			return;
		}
		AsyncSupplier<ByteBuffersIO, IOException> read = readFullyAsync(io, 65536);
		read.thenStart("readFully: convert ByteArraysIO into byte[]", io.getPriority(),
			() -> result.unblockSuccess(read.getResult().createSingleByteArray()), result);
	}
	
	/** Read fully into a byte[], and unblock the given result upon completion. */
	public static <T extends IO.Readable & IO.KnownSize> void readFullyKnownSize(T io, AsyncSupplier<byte[], IOException> result) {
		AsyncSupplier<Long, IOException> getSize = io.getSizeAsync();
		getSize.thenDoOrStart("readFully", io.getPriority(), size -> {
			if (size.longValue() > Integer.MAX_VALUE) {
				result.error(new IOException("IO too large to be read into memory"));
				return;
			}
			readFullyKnownSize(io, size.intValue(), result);
		}, result);
	}

	/** Read fully into a byte[], and unblock the given result upon completion. */
	public static void readFullyKnownSize(IO.Readable io, int size, AsyncSupplier<byte[], IOException> result) {
		byte[] bytes;
		try { bytes = ByteArrayCache.getInstance().get(size, false); }
		catch (Exception t) {
			result.error(IO.error(t));
			return;
		}
		readFullyAsync(io, ByteBuffer.wrap(bytes), r -> {
			if (r.getValue2() != null)
				result.error(r.getValue2());
			else
				result.unblockSuccess(bytes);
		});
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
	public static AsyncSupplier<Integer,IOException> readFullyAsync(
		IO.Readable io, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
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
	public static AsyncSupplier<Integer,IOException> readFullyAsync(
		IO.Readable io, ByteBuffer buffer, int done, Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncSupplier<Integer,IOException> read = io.readAsync(buffer);
		if (!read.isDone())
			return readFullyAsync(read, io, buffer, done, ondone);
		
		if (!read.isSuccessful()) {
			if (ondone != null && read.getError() != null) ondone.accept(new Pair<>(null, read.getError()));
			return read;
		}
		if (!buffer.hasRemaining()) {
			if (done == 0) {
				if (ondone != null) ondone.accept(new Pair<>(read.getResult(), null));
				return read;
			}
			if (read.getResult().intValue() <= 0) return success(Integer.valueOf(done), ondone);
			return success(Integer.valueOf(read.getResult().intValue() + done), ondone);
		}
		if (read.getResult().intValue() <= 0) {
			if (done == 0) {
				if (ondone != null) ondone.accept(new Pair<>(read.getResult(), null));
				return read;
			}
			return success(Integer.valueOf(done), ondone);
		}
		return readFullyAsync(io, buffer, read.getResult().intValue() + done, ondone);
	}
	
	private static AsyncSupplier<Integer,IOException> readFullyAsync(
		AsyncSupplier<Integer,IOException> read, IO.Readable io, ByteBuffer buffer, int done, Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
		MutableInteger total = new MutableInteger(done);
		Mutable<AsyncSupplier<Integer,IOException>> r = new Mutable<>(read);
		read.listen(new RecursiveAsyncSupplierListener<Integer>((result, that) -> {
			do {
				if (!buffer.hasRemaining() || result.intValue() <= 0) {
					if (total.get() == 0)
						success(result, sp, ondone);
					else if (result.intValue() >= 0)
						success(Integer.valueOf(result.intValue() + total.get()), sp, ondone);
					else
						success(Integer.valueOf(total.get()), sp, ondone);
					return;
				}
				total.add(result.intValue());
				AsyncSupplier<Integer, IOException> reading = io.readAsync(buffer);
				r.set(reading);
				if (reading.isSuccessful()) result = reading.getResult();
				else break;
			} while (true);
			r.get().listen(that);
		}, sp, ondone));
		sp.onCancel(cancel -> r.get().unblockCancel(cancel));
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
	public static AsyncSupplier<Integer,IOException> readFullyAsync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncSupplier<Integer,IOException> read = io.readAsync(pos, buffer);
		if (read.isDone()) {
			if (!read.isSuccessful()) {
				if (ondone != null && read.getError() != null) ondone.accept(new Pair<>(null, read.getError()));
				return read;
			}
			if (!buffer.hasRemaining()) {
				if (ondone != null && read.getResult() != null) ondone.accept(new Pair<>(read.getResult(), null));
				return read;
			}
			if (read.getResult().intValue() <= 0) {
				if (ondone != null) ondone.accept(new Pair<>(read.getResult(), null));
				return read;
			}
		}
		return readFullyAsync(read, io, pos, buffer, ondone);
	}
	
	private static AsyncSupplier<Integer,IOException> readFullyAsync(
		AsyncSupplier<Integer,IOException> read, IO.Readable.Seekable io, long pos, ByteBuffer buffer,
		Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
		MutableInteger total = new MutableInteger(0);
		Mutable<AsyncSupplier<Integer,IOException>> r = new Mutable<>(read);
		read.listen(new RecursiveAsyncSupplierListener<Integer>((result, that) -> {
			do {
				if (!buffer.hasRemaining() || result.intValue() <= 0) {
					if (total.get() == 0)
						success(result, sp, ondone);
					else if (result.intValue() >= 0)
						success(Integer.valueOf(result.intValue() + total.get()), sp, ondone);
					else
						success(Integer.valueOf(total.get()), sp, ondone);
					return;
				}
				total.add(result.intValue());
				AsyncSupplier<Integer, IOException> reading = io.readAsync(pos + total.get(), buffer);
				r.set(reading);
				if (reading.isSuccessful()) result = reading.getResult();
				else break;
			} while (true);
			r.get().listen(that);
		}, sp, ondone));
		sp.onCancel(cancel -> r.get().unblockCancel(cancel));
		return sp;
	}
	
	/**
	 * Read the content of IO.Readable in memory and put it into a ByteBuffersIO.
	 */
	public static AsyncSupplier<ByteBuffersIO, IOException> readFullyAsync(IO.Readable io, int bufferSize) {
		ByteBuffersIO bb = new ByteBuffersIO(false, io.getSourceDescription(), io.getPriority());
		AsyncSupplier<ByteBuffersIO, IOException> result = new AsyncSupplier<>();
		readFullyAsync(io, bufferSize, bb, ByteArrayCache.getInstance(), result);
		return result;
	}
	
	private static void readFullyAsync(
		IO.Readable io, int bufferSize, ByteBuffersIO bb, ByteArrayCache cache, AsyncSupplier<ByteBuffersIO, IOException> result
	) {
		byte[] buffer = cache.get(bufferSize, true);
		AsyncSupplier<Integer, IOException> read = io.readFullyAsync(ByteBuffer.wrap(buffer));
		read.thenStart("readFully", io.getPriority(), (Task<Void, NoException> t) -> {
			int nb = read.getResult().intValue();
			if (nb > 0)
				bb.addBuffer(new ByteArray.Writable(buffer, 0, nb, true));
			if (nb < bufferSize)
				result.unblockSuccess(bb);
			else
				readFullyAsync(io, bufferSize, bb, cache, result);
			return null;
		}, result);
	}

	private static final String READING_FROM = "Reading from ";
	
	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static AsyncSupplier<Integer,IOException> readAsyncUsingSync(
		IO.Readable io, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu(READING_FROM + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.readSync(buffer)), ondone).start().getOutput();
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
	public static AsyncSupplier<Integer,IOException> readAsyncUsingSync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu(READING_FROM + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.readSync(pos, buffer)), ondone).start().getOutput();
	}
	
	/**
	 * Implement an asynchronous read using a task calling the synchronous one.
	 * This must be used only if the synchronous read is using CPU.
	 * @param io the readable to read from
	 * @param buffer the buffer to fill
	 * @param ondone a listener to call before to return the result
	 * @return the number of bytes read
	 */
	public static AsyncSupplier<Integer,IOException> readFullyAsyncUsingSync(
		IO.Readable io, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu(READING_FROM + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.readFullySync(buffer)), ondone).start().getOutput();
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
	public static AsyncSupplier<Integer,IOException> readFullyAsyncUsingSync(
		IO.Readable.Seekable io, long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu(READING_FROM + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.readFullySync(pos, buffer)), ondone).start().getOutput();
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
	public static AsyncSupplier<Long,IOException> skipAsyncUsingSync(IO.Readable io, long n, Consumer<Pair<Long,IOException>> ondone) {
		return Task.cpu("Skipping bytes", io.getPriority(), t -> Long.valueOf(skipSyncByReading(io, n)), ondone).start().getOutput();
	}
	
	/**
	 * Implement an asynchronous skip using readAsync.
	 */
	public static AsyncSupplier<Long, IOException> skipAsyncByReading(IO.Readable io, long n, Consumer<Pair<Long,IOException>> ondone) {
		if (n <= 0) {
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(0), null));
			return new AsyncSupplier<>(Long.valueOf(0), null);
		}
		ByteBuffer b = ByteBuffer.allocate(n > 65536 ? 65536 : (int)n);
		MutableLong done = new MutableLong(0);
		AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
		io.readAsync(b).listen(new RecursiveAsyncSupplierListener<Integer>((nb, that) -> {
			AsyncSupplier<Integer, IOException> next;
			do {
				int read = nb.intValue();
				if (read <= 0) {
					IOUtil.success(Long.valueOf(done.get()), result, ondone);
					return;
				}
				done.add(nb.intValue());
				if (done.get() == n) {
					IOUtil.success(Long.valueOf(n), result, ondone);
					return;
				}
				b.clear();
				if (n - done.get() < b.remaining())
					b.limit((int)(n - done.get()));
				next = io.readAsync(b);
				if (next.isSuccessful()) nb = next.getResult();
				else break;
			} while (true);
			next.listen(that);
		}, result, ondone));
		return result;
	}
	
	/**
	 * Write the given Readable to a temporary file, and return the temporary file.
	 * The given IO is closed when done.
	 */
	public static AsyncSupplier<File, IOException> toTempFile(IO.Readable io) {
		IO.Readable.Buffered bio;
		if (io instanceof IO.Readable.Buffered)
			bio = (IO.Readable.Buffered)io;
		else
			bio = new SimpleBufferedReadable(io, 65536);
		AsyncSupplier<FileIO.ReadWrite, IOException> createFile =
			TemporaryFiles.get().createAndOpenFileAsync("net.lecousin.framework.io", "streamtofile");
		AsyncSupplier<File, IOException> result = new AsyncSupplier<>();
		createFile.onDone(() -> {
			File file = createFile.getResult().getFile();
			copy(bio, createFile.getResult(), -1, true, null, 0).onDone(() -> result.unblockSuccess(file), result);
		}, result);
		return result;
	}
	
	/** Create a temporary file with the given content. */
	public static AsyncSupplier<File, IOException> toTempFile(byte[] bytes) {
		AsyncSupplier<FileIO.ReadWrite, IOException> createFile =
				TemporaryFiles.get().createAndOpenFileAsync("net.lecousin.framework.io", "bytestofile");
		AsyncSupplier<File, IOException> result = new AsyncSupplier<>();
		createFile.onDone(() -> {
			File file = createFile.getResult().getFile();
			createFile.getResult().writeAsync(ByteBuffer.wrap(bytes)).onDone(() -> {
				try {
					createFile.getResult().close();
					result.unblockSuccess(file);
				} catch (Exception e) {
					result.error(IO.error(e));
				}
			});
		}, result);
		return result;
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readSyncUsingAsync(IO.Readable io, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.readAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readSyncUsingAsync(IO.Readable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.readAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readFullySyncUsingAsync(IO.Readable io, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.readFullyAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous read using an asynchronous one and blocking until it finishes.
	 */
	public static int readFullySyncUsingAsync(IO.Readable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.readFullyAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous skip using an asynchronous one and blocking until it finishes.
	 */
	public static long skipSyncUsingAsync(IO.Readable io, long n) throws IOException {
		AsyncSupplier<Long,IOException> sp = io.skipAsync(n);
		try { return sp.blockResult(0).longValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous write using an asynchronous one and blocking until it finishes.
	 */
	public static int writeSyncUsingAsync(IO.Writable io, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.writeAsync(buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement a synchronous write using an asynchronous one and blocking until it finishes.
	 */
	public static int writeSyncUsingAsync(IO.Writable.Seekable io, long pos, ByteBuffer buffer) throws IOException {
		AsyncSupplier<Integer,IOException> sp = io.writeAsync(pos, buffer);
		try { return sp.blockResult(0).intValue(); }
		catch (CancelException e) { throw IO.errorCancelled(e); }
	}
	
	/**
	 * Implement an asynchronous write using a task calling a synchronous write.
	 * This must be used only if the synchronous write is only using CPU.
	 */
	public static AsyncSupplier<Integer,IOException> writeAsyncUsingSync(
		IO.Writable io, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu("Writing to " + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.writeSync(buffer)), ondone).start().getOutput();
	}
	
	/**
	 * Implement an asynchronous write using a task calling a synchronous write.
	 * This must be used only if the synchronous write is only using CPU.
	 */
	public static AsyncSupplier<Integer,IOException> writeAsyncUsingSync(
		IO.Writable.Seekable io, long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return Task.cpu("Writing to " + io.getSourceDescription(), io.getPriority(),
			t -> Integer.valueOf(io.writeSync(pos, buffer)), ondone).start().getOutput();
	}
	
	/**
	 * Read all bytes from the given Readable and convert it as a String using the given charset encoding.
	 */
	public static AsyncSupplier<CharArrayStringBuffer,IOException> readFullyAsString(IO.Readable io, Charset charset, Priority priority) {
		AsyncSupplier<CharArrayStringBuffer,IOException> result = new AsyncSupplier<>();
		if (io instanceof IO.KnownSize) {
			((IO.KnownSize)io).getSizeAsync().onDone(size ->
				Task.cpu("Prepare readFullyAsString", priority, t -> {
					byte[] buf = new byte[size.intValue()];
					io.readFullyAsync(ByteBuffer.wrap(buf)).thenStart(
					Task.cpu("readFullyAsString", priority, t2 -> {
						try {
							result.unblockSuccess(new CharArrayStringBuffer(
								charset.newDecoder().decode(ByteBuffer.wrap(buf))
							));
						} catch (IOException e) {
							result.error(e);
						}
						return null;
					}), result);
					return null;
				}).start(),
			result);
			return result;
		}
		Task.cpu("Read file as string: " + io.getSourceDescription(), priority, t -> {
			BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, charset, 1024, 128);
			CharArrayStringBuffer str = new CharArrayStringBuffer();
			readFullyAsString(stream, str, result, priority);
			return null;
		}).startOn(io.canStartReading(), true);
		return result;
	}
	
	private static void readFullyAsString(
		BufferedReadableCharacterStream stream, CharArrayStringBuffer str,
		AsyncSupplier<CharArrayStringBuffer,IOException> result, Priority priority
	) {
		do {
			AsyncSupplier<Chars.Readable, IOException> read = stream.readNextBufferAsync();
			if (read.isDone()) {
				if (read.hasError()) {
					result.error(read.getError());
					return;
				}
				if (read.getResult() == null) {
					result.unblockSuccess(str);
					return;
				}
				read.getResult().get(str, read.getResult().remaining());
				continue;
			}
			read.thenStart("readFullyAsString: " + stream.getDescription(), priority, (Task<Void, NoException> t) -> {
				if (read.getResult() == null) {
					result.unblockSuccess(str);
					return null;
				}
				read.getResult().get(str, read.getResult().remaining());
				readFullyAsString(stream, str, result, priority);
				return null;
			}, result);
			return;
		} while (true);
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
		try (FileIO.ReadOnly io = new FileIO.ReadOnly(file, Task.Priority.RATHER_IMPORTANT)) {
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
	public static AsyncSupplier<Long,IOException> seekAsyncUsingSync(
		IO.Seekable io, SeekType type, long move, Consumer<Pair<Long,IOException>> ondone
	) {
		return Task.cpu("Seeking", io.getPriority(), t -> Long.valueOf(io.seekSync(type, move)), ondone).start().getOutput();
	}
	
	/**
	 * Implement a synchronous sync by calling an asynchronous one and blocking until the result is available.
	 */
	public static long seekSyncUsingAsync(IO.Seekable io, SeekType type, long move) throws IOException {
		AsyncSupplier<Long,IOException> seek = io.seekAsync(type, move);
		seek.blockException(0);
		return seek.getResult().longValue();
	}
	
	/**
	 * Implement an asynchronous setSize using a task calling a synchronous one.
	 * This must be used only if the synchronous setSize is only using CPU.
	 */
	public static IAsync<IOException> setSizeAsyncUsingSync(IO.Resizable io, long newSize, Priority priority) {
		return Task.cpu("Resizing " + io.getSourceDescription(), priority, (Task<Void, IOException> t) -> {
			io.setSizeSync(newSize);
			return null;
		}).start().getOutput();
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
	public static AsyncSupplier<Long, IOException> copy(
		IO.Readable input, IO.Writable output, long size, boolean closeIOs, WorkProgress progress, long work
	) {
		AsyncSupplier<Long, IOException> sp = new AsyncSupplier<>();
		copy(input, output, size, closeIOs, sp, progress, work);
		return sp;
	}
	
	private static void copy(
		IO.Readable input, IO.Writable output, long size, boolean closeIOs,
		AsyncSupplier<Long, IOException> sp, WorkProgress progress, long work
	) {
		if (size == 0) {
			if (progress != null) progress.progress(work);
			copyEnd(input, output, sp, null, null, closeIOs, 0);
			return;
		}
		if (size < 0) {
			if (input instanceof IO.KnownSize) {
				AsyncSupplier<Long, IOException> getSize = ((IO.KnownSize)input).getSizeAsync();
				getSize.onDone(() -> {
					if (getSize.hasError())
						copyEnd(input, output, sp, getSize.getError(), null, closeIOs, 0);
					else if (getSize.isCancelled())
						copyEnd(input, output, sp, null, getSize.getCancelEvent(), closeIOs, 0);
					else
						copy(input, output, getSize.getResult().longValue(), closeIOs, sp, progress, work);
				});
				return;
			}
			// unknown size
			copyUnknownSize(input, output, closeIOs, sp, progress, work);
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
	
	private static void copyUnknownSize(
		IO.Readable input, IO.Writable output, boolean closeIOs, AsyncSupplier<Long, IOException> sp, WorkProgress progress, long work
	) {
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
	}
	
	private static void copyEnd(
		IO.Readable input, IO.Writable output,
		AsyncSupplier<Long, IOException> sp, IOException error, CancelException cancel,
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
		IAsync<IOException> sp1 = input.closeAsync();
		IAsync<IOException> sp2 = output.closeAsync();
		JoinPoint.from(sp1, sp2).onDone(() -> {
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
		});
	}

	
	@SuppressWarnings("squid:S00107")
	private static void copySameTM(
		IO.Readable input, IO.Writable output, int bufferSize, long total, 
		AsyncSupplier<Long,IOException> end, boolean closeIOs, WorkProgress progress, long work
	) {
		Task.cpu("Allocate buffers to copy IOs", input.getPriority(), t -> {
			copySameTMStep(input, output, ByteBuffer.allocate(bufferSize), 0, total, end, closeIOs, progress, work);
			return null;
		}).start();
	}
	
	@SuppressWarnings("squid:S00107")
	private static void copySameTMStep(
		IO.Readable input, IO.Writable output, ByteBuffer buf, long written, long total,
		AsyncSupplier<Long,IOException> end, boolean closeIOs, WorkProgress progress, long work
	) {
		input.readFullyAsync(buf).listen(new Listener<Integer, IOException>() {
			@Override
			public void ready(Integer result) {
				int nb = result.intValue();
				if (nb <= 0) {
					if (progress != null) progress.progress(work);
					copyEnd(input, output, end, null, null, closeIOs, written);
				} else {
					buf.flip();
					if (progress != null) progress.progress(nb * work / (total * 2));
					AsyncSupplier<Integer,IOException> write = output.writeAsync(buf);
					write.listen(new Listener<Integer, IOException>() {
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
		IO.Readable.Buffered input, IO.Writable output, AsyncSupplier<Long, IOException> sp, long written,
		boolean closeIOs, WorkProgress progress, long work
	) {
		AsyncSupplier<ByteBuffer, IOException> read = input.readNextBufferAsync();
		read.onDone(() -> {
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
			AsyncSupplier<Integer, IOException> write = output.writeAsync(buf);
			write.onDone(() -> {
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
			});
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
	Async<IOException> copy(T io, long src, long dst, long len) {
		Async<IOException> sp = new Async<>();
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
	void copy(T io, long src, long dst, ByteBuffer buffer, long len, Async<IOException> sp) {
		io.readFullyAsync(src, buffer).onDone(() -> {
			buffer.flip();
			AsyncSupplier<Integer, IOException> write = io.writeAsync(dst, buffer);
			if (len <= 4 * 1024 * 1024)
				write.onDone(sp);
			else write.onDone(() -> {
				long nl = len - 4 * 1024 * 1024;
				buffer.clear();
				if (nl < 4 * 1024 * 1024) buffer.limit((int)nl);
				copy(io, src + 4 * 1024 * 1024, dst + 4 * 1024 * 1024, buffer, nl, sp);
			});
		});
	}

	
	// ---- copy files ----
	
	/** Copy a file. */
	public static AsyncSupplier<Long, IOException> copy(
		File src, File dst, Priority priority, long knownSize, WorkProgress progress, long work, IAsync<?> startOn
	) {
		AsyncSupplier<Long, IOException> sp = new AsyncSupplier<>();
		Task<Void,NoException> task = Task.cpu("Start copying files", priority, t -> {
			FileIO.ReadOnly input = new FileIO.ReadOnly(src, priority);
			FileIO.WriteOnly output = new FileIO.WriteOnly(dst, priority);
			input.canStart().onDone(() -> {
				if (input.canStart().hasError()) {
					copyEnd(input, output, sp,
						new IOException("Unable to open file " + src.getAbsolutePath(),
							input.canStart().getError()),
						null, true, 0);
					return;
				}
				output.canStartWriting().onDone(() -> {
					if (output.canStartWriting().hasError()) {
						copyEnd(input, output, sp,
							new IOException("Unable to open file " + dst.getAbsolutePath(),
								input.canStart().getError()),
							null, true, 0);
						return;
					}
					copy(input, output, knownSize, true, sp, progress, work);
				});
			});
			return null;
		});
		if (startOn == null)
			task.start();
		else
			task.startOn(startOn, true);
		return sp;
	}

	

	/** Implement an asynchronous close using a task calling the synchronous close. */
	public static IAsync<IOException> closeAsync(Closeable toClose) {
		return Task.cpu("Closing resource", Task.Priority.RATHER_IMPORTANT, (Task<Void, IOException> t) -> {
			toClose.close();
			return null;
		}).start().getOutput();
	}
	
	/** Read the content of a file and return a byte array. */
	@SuppressWarnings("squid:S2095") // f is closed
	public static AsyncSupplier<byte[], IOException> readFully(File file, Priority priority) {
		AsyncSupplier<byte[], IOException> result = new AsyncSupplier<>();
		FileIO.ReadOnly f = new FileIO.ReadOnly(file, priority);
		f.getSizeAsync().listen(new Listener<Long, IOException>() {
			@Override
			public void error(IOException error) {
				try { f.close(); } catch (Exception e) { /* ignore */ }
				result.error(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				try { f.close(); } catch (Exception e) { /* ignore */ }
				result.cancel(event);
			}
			
			@Override
			public void ready(Long size) {
				byte[] buf = new byte[size.intValue()];
				f.readFullyAsync(ByteBuffer.wrap(buf)).listen(new Listener<Integer, IOException>() {
					@Override
					public void error(IOException error) {
						try { f.close(); } catch (Exception e) { /* ignore */ }
						result.error(error);
					}
					
					@Override
					public void cancelled(CancelException event) {
						try { f.close(); } catch (Exception e) { /* ignore */ }
						result.cancel(event);
					}
					
					@Override
					public void ready(Integer read) {
						if (read.intValue() != buf.length)
							result.error(new IOException(
								"Only " + read.intValue() + " bytes read on file size " + buf.length));
						else {
							try { f.close(); } catch (Exception e) { /* ignore */ }
							result.unblockSuccess(buf);
						}
					}
				});
			}
		});
		return result;
	}
	
	
	/** Get the underlying task manager by going through the wrapped IO. */
	public static TaskManager getUnderlyingTaskManager(IO io) {
		io = getUnderlyingIO(io);
		return io.getTaskManager();
	}
	
	/** Get the underlying IO by going through the wrapped IO until the last one. */
	public static IO getUnderlyingIO(IO io) {
		do {
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
		AsyncSupplier<T, IOException> toListen, AsyncSupplier<T, IOException> toUnblock, Consumer<Pair<T,IOException>> ondone
	) {
		toListen.onDone(
			result -> {
				if (ondone != null) ondone.accept(new Pair<>(result, null));
				toUnblock.unblockSuccess(result);
			},
			error -> {
				if (ondone != null) ondone.accept(new Pair<>(null, error));
				toUnblock.error(error);
			},
			toUnblock::cancel
		);
	}

	/** Listen to <code>toListen</code>, then calls <code>onReady</code> if it succeed.
	 * In case of error or cancellation, the error or cancellation event is given to <code>onErrorOrCancel</code>.
	 * If ondone is not null, it is called before <code>onReady</code> and <code>onErrorOrCancel</code>.
	 */
	public static <T, T2> void listenOnDone(
		AsyncSupplier<T, IOException> toListen, Consumer<T> onReady, IAsync<IOException> onErrorOrCancel,
		Consumer<Pair<T2,IOException>> ondone
	) {
		toListen.onDone(
			onReady,
			error -> {
				if (ondone != null) ondone.accept(new Pair<>(null, error));
				onErrorOrCancel.error(error);
			},
			onErrorOrCancel::cancel
		);
	}
	
	/** Listen to <code>toListen</code>, then launch <code>onReady</code> if it succeed.
	 * In case of error or cancellation, the error or cancellation event is given to <code>onErrorOrCancel</code>.
	 * If ondone is not null, it is called before <code>onReady</code> and <code>onErrorOrCancel</code>.
	 */
	public static <T, T2> void listenOnDone(
		AsyncSupplier<T, IOException> toListen, Task<?,?> onReady, IAsync<IOException> onErrorOrCancel,
		Consumer<Pair<T2,IOException>> ondone
	) {
		toListen.onDone(() -> {
			if (toListen.isCancelled()) {
				if (onErrorOrCancel != null) onErrorOrCancel.cancel(toListen.getCancelEvent());
			} else if (toListen.hasError()) {
				if (ondone != null) ondone.accept(new Pair<>(null, toListen.getError()));
				if (onErrorOrCancel != null) onErrorOrCancel.error(toListen.getError());
			} else {
				onReady.start();
			}
		});
	}
	
	/** Listen to <code>toListen</code>, then calls <code>onReady</code> if it succeed.
	 * In case of error or cancellation, the error or cancellation event is given to <code>onErrorOrCancel</code>.
	 * If ondone is not null, it is called before <code>onReady</code> and <code>onErrorOrCancel</code>.
	 */
	public static <T> void listenOnDone(
		IAsync<IOException> toListen, Runnable onReady, IAsync<IOException> onErrorOrCancel,
		Consumer<Pair<T,IOException>> ondone
	) {
		toListen.onDone(
			onReady,
			error -> {
				if (ondone != null) ondone.accept(new Pair<>(null, error));
				onErrorOrCancel.error(error);
			},
			onErrorOrCancel::cancel
		);
	}
	
	/** Create a consumer that call forward the error to the given sync and call onDone. */ 
	public static <T> Consumer<IOException> errorConsumer(IAsync<IOException> async, Consumer<Pair<T, IOException>> ondone) {
		return error -> {
			async.error(error);
			ondone.accept(new Pair<>(null, error));
		};
	}
	
	/** Recursive listener, forwarding error or cancel and calling onDone on error.
	 * @param <T> type of result
	 */
	public static class RecursiveAsyncSupplierListener<T> implements AsyncSupplier.Listener<T, IOException> {
		
		/** On success listener that receive the listener instance to perform recursive operation.
		 * @param <T> type of result
		 */
		public static interface OnSuccess<T> {
			/** On success listener. */
			void accept(T result, RecursiveAsyncSupplierListener<T> listener);
		}

		/** Constructor. */
		public <T2> RecursiveAsyncSupplierListener(
			OnSuccess<T> onSuccess,
			IAsync<IOException> onErrorOrCancel,
			Consumer<Pair<T2, IOException>> ondone
		) {
			this.onSuccess = onSuccess;
			this.onErrorOrCancel = onErrorOrCancel;
			this.ondone = ondone;
		}
		
		private OnSuccess<T> onSuccess;
		private IAsync<IOException> onErrorOrCancel;
		@SuppressWarnings("rawtypes")
		private Consumer ondone;
		
		@Override
		public void ready(T result) {
			onSuccess.accept(result, this);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void error(IOException error) {
			if (ondone != null) ondone.accept(new Pair<>(null, error));
			if (onErrorOrCancel != null) onErrorOrCancel.error(error);
		}
		
		@Override
		public void cancelled(CancelException event) {
			if (onErrorOrCancel != null) onErrorOrCancel.cancel(event);
		}
		
		@Override
		public String toString() {
			return "IOUtil$RecursiveAsyncSupplierListener: " + onSuccess + " / " + ondone + " / " + onErrorOrCancel;
		}
	}
	
	/** Shortcut to transfer an error to ondone if it is not null, and to the AsyncWork. */
	public static <TResult, TError extends Exception>
	void error(TError error, AsyncSupplier<TResult, TError> result, Consumer<Pair<TResult, TError>> ondone) {
		if (ondone != null) ondone.accept(new Pair<>(null, error));
		result.error(error);
	}
	
	/** Shortcut to transfer an error to ondone if it is not null, and create an AsyncWork with this error. */
	public static <TResult, TError extends Exception>
	AsyncSupplier<TResult, TError> error(TError error, Consumer<Pair<TResult, TError>> ondone) {
		if (ondone != null) ondone.accept(new Pair<>(null, error));
		return new AsyncSupplier<>(null, error);
	}
	
	/** Shortcut to transfer a result to ondone if it is not null, and to the AsyncWork. */
	public static <TResult, TError extends Exception>
	void success(TResult res, AsyncSupplier<TResult, TError> result, Consumer<Pair<TResult, TError>> ondone) {
		if (ondone != null) ondone.accept(new Pair<>(res, null));
		result.unblockSuccess(res);
	}
	
	/** Shortcut to transfer a result to ondone if it is not null, and create an AsyncWork with this result. */
	public static <TResult, TError extends Exception>
	AsyncSupplier<TResult, TError> success(TResult result, Consumer<Pair<TResult, TError>> ondone) {
		if (ondone != null) ondone.accept(new Pair<>(result, null));
		return new AsyncSupplier<>(result, null);
	}
	
	/** Shortcut to transfer error or cancellation. */
	public static <TResult, TError extends Exception>
	void notSuccess(IAsync<TError> sp, AsyncSupplier<TResult, TError> result, Consumer<Pair<TResult, TError>> ondone) {
		if (sp.hasError())
			error(sp.getError(), result, ondone);
		else
			result.cancel(sp.getCancelEvent());
	}
	
	/** Get the size by seeking at the end if not an instanceof IO.KnownSize. */
	public static long getSizeSync(IO.Readable.Seekable io) throws IOException {
		if (io instanceof IO.KnownSize)
			return ((IO.KnownSize)io).getSizeSync();
		long pos = io.getPosition();
		long size = io.seekSync(SeekType.FROM_END, 0);
		io.seekSync(SeekType.FROM_BEGINNING, pos);
		return size;
	}
}
