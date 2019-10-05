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
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestIOError extends LCCoreAbstractTest {

	public static class IOError1 extends ConcurrentCloseable<IOException> implements IO.Readable, IO.Readable.Seekable, IO.Readable.Buffered {

		protected IOException error = new IOException("it's normal");
		
		@Override
		public String getSourceDescription() {
			return "IOError1";
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
		
	}
	
	protected abstract IO.Readable getReadable(IOError1 io) throws Exception;

	protected abstract IO.Readable.Buffered getReadableBuffered(IOError1 io) throws Exception;

	protected abstract IO.Readable.Seekable getReadableSeekable(IOError1 io) throws Exception;
	
	@Test(timeout=120000)
	public void testsReadable() throws Exception {
		IO.Readable io = getReadable(new IOError1());
		Assume.assumeNotNull(io);
		io.close();
		
		io = getReadable(new IOError1());
		try {
			io.readSync(ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadable(new IOError1());
		try {
			io.readFullySync(ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadable(new IOError1());
		try {
			io.skipSync(10);
			// may be ok...
		} catch (IOException e) { /* ok */ }
		io.close();
		
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		Consumer<Pair<Long, IOException>> ondoneL = p -> called.set(p.getValue2() != null);
		
		io = getReadable(new IOError1());
		try {
			io.readAsync(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new IOError1());
		try {
			io.readAsync(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
		
		io = getReadable(new IOError1());
		try {
			io.readFullyAsync(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new IOError1());
		try {
			io.readFullyAsync(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
		
		io = getReadable(new IOError1());
		try {
			io.skipAsync(10).blockResult(30000);
			// may be ok...
		} catch (IOException | CancelException e) { /* ok */ }
		io.close();
		io = getReadable(new IOError1());
		try {
			io.skipAsync(10, ondoneL).blockResult(30000);
			// may be ok...
			called.set(true);
		} catch (IOException | CancelException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
	@Test(timeout=120000)
	public void testsReadableSeekable() throws Exception {
		IO.Readable.Seekable io = getReadableSeekable(new IOError1());
		Assume.assumeNotNull(io);
		io.close();
		
		io = getReadableSeekable(new IOError1());
		try {
			io.readSync(10, ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		
		io = getReadableSeekable(new IOError1());
		try {
			io.readFullySync(10, ByteBuffer.allocate(1024));
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();

		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		
		io = getReadableSeekable(new IOError1());
		try {
			io.readAsync(10, ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableSeekable(new IOError1());
		try {
			io.readAsync(10, ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();

		io = getReadableSeekable(new IOError1());
		try {
			io.readFullyAsync(10, ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableSeekable(new IOError1());
		try {
			io.readFullyAsync(10, ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
	@Test(timeout=120000)
	public void testsReadableBuffered() throws Exception {
		IO.Readable.Buffered io = getReadableBuffered(new IOError1());
		Assume.assumeNotNull(io);
		io.close();
		
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = p -> called.set(p.getValue2() != null);
		Consumer<Pair<ByteBuffer, IOException>> ondoneBB = p -> called.set(p.getValue2() != null);
		
		io = getReadableBuffered(new IOError1());
		try {
			if (io.readAsync() == -2) {
				io.canStartReading().block(20000);
				io.readAsync();
			}
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();

		io = getReadableBuffered(new IOError1());
		try {
			io.readNextBufferAsync().blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableBuffered(new IOError1());
		try {
			io.readNextBufferAsync(ondoneBB).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();

		io = getReadableBuffered(new IOError1());
		try {
			io.readFullySyncIfPossible(ByteBuffer.allocate(1024)).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		io.close();
		io = getReadableBuffered(new IOError1());
		try {
			io.readFullySyncIfPossible(ByteBuffer.allocate(1024), ondone).blockResult(30000);
			throw new AssertionError();
		} catch (IOException e) { /* ok */ }
		Assert.assertTrue(called.get());
		called.set(false);
		io.close();
	}
	
}
