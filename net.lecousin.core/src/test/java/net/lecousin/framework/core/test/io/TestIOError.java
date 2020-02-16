package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestIOError extends LCCoreAbstractTest {

	/** Return an IOException for all operations. */
	public static class ReadableAlwaysError extends ConcurrentCloseable<IOException> implements IO.Readable, IO.Readable.Seekable, IO.Readable.Buffered {

		protected IOException error = new IOException("it's normal");
		
		@Override
		public String getSourceDescription() {
			return getClass().getSimpleName();
		}

		@Override
		public IO getWrappedIO() {
			return null;
		}

		@Override
		public void setPriority(byte priority) {
		}

		@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}

		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public long getPosition() throws IOException {
			return 0;
		}

		@Override
		public int read() throws IOException {
			throw error;
		}

		@Override
		public int read(byte[] buffer, int offset, int len) throws IOException {
			throw error;
		}

		@Override
		public int readFully(byte[] buffer) throws IOException {
			throw error;
		}

		@Override
		public int skip(int skip) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}
		
		@Override
		public ByteBuffer readNextBuffer() throws IOException {
			throw error;
		}

		@Override
		public int readAsync() throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return new Async<>(true);
		}

		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public long skipSync(long n) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public byte getPriority() {
			return Task.PRIORITY_NORMAL;
		}

		@Override
		protected IAsync<IOException> closeUnderlyingResources() {
			return new Async<>(true);
		}

		@Override
		protected void closeResources(Async<IOException> ondone) {
			ondone.unblock();
		}
		
		public static class KnownSizeAlwaysError extends ReadableAlwaysError implements IO.KnownSize {

			@Override
			public long getSizeSync() throws IOException {
				throw error;
			}

			@Override
			public AsyncSupplier<Long, IOException> getSizeAsync() {
				return new AsyncSupplier<>(null, error);
			}
			
		}
		
	}
	
	public static class WritableAlwaysError extends ConcurrentCloseable<IOException> implements IO.Writable, IO.WritableByteStream {
		
		protected IOException error = new IOException("It's normal");

		@Override
		public String getSourceDescription() {
			return getClass().getName();
		}

		@Override
		public IO getWrappedIO() {
			return null;
		}

		@Override
		public void setPriority(byte priority) {
		}

		@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}

		@Override
		public void write(byte b) throws IOException {
			throw error;
		}

		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			throw error;
		}

		@Override
		public IAsync<IOException> canStartWriting() {
			return new Async<>(true);
		}

		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			throw error;
		}

		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return IOUtil.error(error, ondone);
		}

		@Override
		public byte getPriority() {
			return Task.PRIORITY_NORMAL;
		}

		@Override
		protected IAsync<IOException> closeUnderlyingResources() {
			return new Async<>(true);
		}

		@Override
		protected void closeResources(Async<IOException> ondone) {
			ondone.unblock();
		}
		
	}
	
	/** Return an IOException after first bytes. */
	public static class ReadableErrorAfterBeginning extends ConcurrentCloseable<IOException> implements IO.Readable, IO.Readable.Buffered {

		public ReadableErrorAfterBeginning(ByteArray beginning) {
			this.beginning = beginning;
		}
		
		protected IOException error = new IOException("it's normal");
		protected ByteArray beginning;
		
		@Override
		public String getSourceDescription() {
			return getClass().getSimpleName();
		}

		@Override
		public IO getWrappedIO() {
			return null;
		}

		@Override
		public void setPriority(byte priority) {
		}

		@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}

		@Override
		public int read() throws IOException {
			if (!beginning.hasRemaining())
				throw error;
			return beginning.get() & 0xFF;
		}

		@Override
		public int read(byte[] buffer, int offset, int len) throws IOException {
			if (!beginning.hasRemaining())
				throw error;
			int l = Math.min(len, beginning.remaining());
			beginning.get(buffer, offset, l);
			return l;
		}

		@Override
		public int readFully(byte[] buffer) throws IOException {
			return read(buffer, 0, buffer.length);
		}

		@Override
		public int skip(int skip) throws IOException {
			if (!beginning.hasRemaining())
				throw error;
			int l = Math.min(skip, beginning.remaining());
			beginning.moveForward(l);
			return l;
		}

		@Override
		public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
			if (!beginning.hasRemaining())
				return IOUtil.error(error, ondone);
			ByteBuffer b = beginning.toByteBuffer();
			beginning.goToEnd();
			return IOUtil.success(b, ondone);
		}
		
		@Override
		public ByteBuffer readNextBuffer() throws IOException {
			if (!beginning.hasRemaining())
				throw error;
			ByteBuffer b = beginning.toByteBuffer();
			beginning.goToEnd();
			return b;
		}

		@Override
		public int readAsync() throws IOException {
			return read();
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			if (!beginning.hasRemaining())
				return IOUtil.error(error, ondone);
			int l = Math.min(beginning.remaining(), buffer.remaining());
			buffer.put(beginning.getArray(), beginning.getCurrentArrayOffset(), l);
			beginning.moveForward(l);
			return IOUtil.success(Integer.valueOf(l), ondone);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return new Async<>(true);
		}

		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			if (!beginning.hasRemaining())
				throw error;
			int l = Math.min(beginning.remaining(), buffer.remaining());
			buffer.put(beginning.getArray(), beginning.getCurrentArrayOffset(), l);
			beginning.moveForward(l);
			return l;
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return readFullySyncIfPossible(buffer, ondone);
		}

		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return readSync(buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return readAsync(buffer, ondone);
		}

		@Override
		public long skipSync(long n) throws IOException {
			return skip((int)n);
		}

		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return IOUtil.skipAsyncUsingSync(this, n, ondone);
		}

		@Override
		public byte getPriority() {
			return Task.PRIORITY_NORMAL;
		}

		@Override
		protected IAsync<IOException> closeUnderlyingResources() {
			return new Async<>(true);
		}

		@Override
		protected void closeResources(Async<IOException> ondone) {
			ondone.unblock();
		}
		
	}
	
	protected abstract IO.Readable getReadable(IO.Readable io) throws Exception;

	protected abstract IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception;

	protected abstract IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) throws Exception;
	
	protected abstract IO.Writable getWritable(IO.Writable io) throws Exception;
	
	@Test
	public void testsReadableAlwaysError() throws Exception {
		IO.Readable io = getReadable(new ReadableAlwaysError());
		Assume.assumeNotNull(io);
		io.close();
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readSync(ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readFullySync(ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.skipSync(10);
			// may be ok...
		} catch (IOException e) { /* ok */ }
		io.close();
		
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		Consumer<Pair<Long, IOException>> ondoneL = p -> called.set(p.getValue2() != null);
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readAsync(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readAsync(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readFullyAsync(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new ReadableAlwaysError());
		try {
			io.readFullyAsync(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
		
		io = getReadable(new ReadableAlwaysError());
		try {
			io.skipAsync(10).blockResult(30000);
			// may be ok...
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new ReadableAlwaysError());
		try {
			io.skipAsync(10, ondoneL).blockResult(30000);
			// may be ok...
			called.set(true);
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
	@Test
	public void testsReadableSeekableAlwaysError() throws Exception {
		IO.Readable.Seekable io = getReadableSeekable(new ReadableAlwaysError());
		Assume.assumeNotNull(io);
		io.close();
		
		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readSync(10, ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readFullySync(10, ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();

		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		
		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readAsync(10, ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readAsync(10, ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();

		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readFullyAsync(10, ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableSeekable(new ReadableAlwaysError());
		try {
			io.readFullyAsync(10, ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
	@Test
	public void testsReadableBufferedAlwaysError() throws Exception {
		IO.Readable.Buffered io = getReadableBuffered(new ReadableAlwaysError());
		Assume.assumeNotNull(io);
		io.close();
		
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		Consumer<Pair<ByteBuffer, IOException>> ondoneBB = p -> called.set(p.getValue2() != null);
		
		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			if (io.readAsync() == -2) {
				io.canStartReading().block(20000);
				io.readAsync();
			}
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();

		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			io.readNextBufferAsync().blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			io.readNextBufferAsync(ondoneBB).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();

		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			io.readFullySyncIfPossible(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			io.readFullySyncIfPossible(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();

		io = getReadableBuffered(new ReadableAlwaysError());
		try {
			io.readNextBuffer();
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
	}
	
	@Test
	public void testWritableAlwaysError() throws Exception {
		IO.Writable io = getWritable(new WritableAlwaysError());
		Assume.assumeNotNull(io);
		io.close();
		
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);

		io = getWritable(new WritableAlwaysError());
		try {
			io.writeSync(ByteBuffer.allocate(100));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getWritable(new WritableAlwaysError());
		try {
			io.writeAsync(ByteBuffer.allocate(100)).blockResult(0);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getWritable(new WritableAlwaysError());
		try {
			io.writeAsync(ByteBuffer.allocate(100), ondone).blockResult(0);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
}
