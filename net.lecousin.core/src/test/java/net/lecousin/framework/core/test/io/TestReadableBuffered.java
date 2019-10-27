package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.ReadableByteStream;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestReadableBuffered extends TestReadableByteStream {
	
	public static synchronized List<Object[]> generateTestCases(boolean faster) {
		List<Object[]> cases = TestIO.UsingGeneratedTestFiles.generateTestCases(faster);
		ArrayList<Object[]> result = new ArrayList<>(cases.size() * testBufferingSize.length);
		for (int i = 0; i < testBufferingSize.length; ++i) {
			List<Object[]> list;
			if (testBufferingSize[i] >= 64)
				list = cases;
			else {
				list = TestIO.UsingGeneratedTestFiles.generateTestCases(true);
				for (Object[] params : list)
					if (((Integer)params[2]).intValue() > 20000) {
						params[0] = getFile20000();
						params[1] = testBuf20000;
						params[2] = Integer.valueOf(20000);
					}
			}
			for (Object[] params : list) {
				Object[] newParams = new Object[params.length + 1];
				System.arraycopy(params, 0, newParams, 0, params.length);
				newParams[params.length] = Integer.valueOf(testBufferingSize[i]);
				result.add(newParams);
			}
		}
		return result;
	}
	
	private static File file20000;
	private static byte[] testBuf20000 = "0123456789$ABCDEFGHIJKLMNOPQRSTUVWXYZ-abcdefghijklmnopqrstuvwxyz*9876543210\r\n".getBytes();
	
	private static File getFile20000() {
		if (file20000 == null)
			try {
				file20000 = generateFile(testBuf20000, 20000);
			} catch (IOException e) {
				throw new RuntimeException("Unable to generate file", e);
			}
		return file20000;
	}
	
	public static List<Object[]> addBufferingSize(Collection<Object[]> cases) {
		ArrayList<Object[]> result = new ArrayList<>(cases.size() * testBufferingSize.length);
		for (int i = 0; i < testBufferingSize.length; ++i) {
			for (Object[] params : cases) {
				Object[] newParams = new Object[params.length + 1];
				System.arraycopy(params, 0, newParams, 0, params.length);
				newParams[params.length] = Integer.valueOf(testBufferingSize[i]);
				result.add(newParams);
			}
		}
		return result;
	}
	
	public static int[] testBufferingSize = {
		65536,
		1024,
		2
	};
	
	protected TestReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferingSize) {
		super(testFile, testBuf, nbBuf);
		this.bufferingSize = bufferingSize;
	}
	
	protected int bufferingSize;

	protected abstract IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception;

	@Override
	protected ReadableByteStream createReadableByteStreamFromFile(ReadOnly file, long fileSize) throws Exception {
		return createReadableBufferedFromFile(file, fileSize, bufferingSize);
	}
	
	@Test(timeout=240000)
	public void testReadableBufferedByteByByteAsync() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize(), bufferingSize);
		MutableInteger i = new MutableInteger(0);
		MutableInteger j = new MutableInteger(0);
		Async<Exception> sp = new Async<>();
		Runnable run = new Runnable() {
			@Override
			public void run() {
				if (i.get() >= nbBuf) {
					int c;
					try { c = io.readAsync(); }
					catch (Exception e) {
						sp.error(e);
						return;
					}
					if (c == -1) {
						sp.unblock();
						return;
					}
					if (c == -2) {
						io.canStartReading().thenStart(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), true);
						return;
					}
					sp.error(new Exception("Remaining byte(s) at the end of the file"));
					return;
				}
				if (nbBuf > 1000 && (i.get() % 100) == 99) {
					// make the test faster
					int skipBuf = 50;
					if (i.get() + skipBuf > nbBuf) skipBuf = nbBuf - i.get();
					i.add(skipBuf);
					j.set(0);
					io.skipAsync(skipBuf * testBuf.length).thenStart(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), sp, e -> e);
					return;
				}
				while (j.get() < testBuf.length) {
					int c;
					try { c = io.readAsync(); }
					catch (Exception e) {
						sp.error(e);
						return;
					}
					if (c == -2) {
						io.canStartReading().thenStart(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), true);
						return;
					}
					if (c == -1) {
						sp.error(new Exception("Unexpected end at offset " + (testBuf.length*i.get()+j.get())));
						return;
					}
					if (c != testBuf[j.get()]) {
						sp.error(new Exception("Byte " + c + " read instead of " + (testBuf[j.get()] & 0xFF) + " at offset " + (testBuf.length*i.get()+j.get())));
						return;
					}
					j.inc();
				}
				j.set(0);
				i.inc();
				new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this).start();
			}
		};
		new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), run).start();
		sp.blockException(0);
		io.close();
	}
	
	@Test(timeout=240000)
	public void testReadableBufferedNextBufferAsync() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize(), bufferingSize);
		Async<Exception> done = new Async<>();
		MutableInteger pos = new MutableInteger(0);
		Mutable<AsyncSupplier<ByteBuffer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<ByteBuffer,IOException>> ondone = param -> onDoneBefore.set(true);
		read.set(io.readNextBufferAsync(ondone));
		read.get().onDone(new Runnable() {
			@Override
			public void run() {
				do {
					if (!onDoneBefore.get()) {
						done.error(new Exception("Method readNextBufferAsync didn't call ondone before listeners"));
						return;
					}
					onDoneBefore.set(false);
					AsyncSupplier<ByteBuffer,IOException> res = read.get();
					if (res.hasError()) {
						done.error(res.getError());
						return;
					}
					int p = pos.get();
					ByteBuffer buffer = res.getResult();
					if (p == testBuf.length * nbBuf) {
						if (buffer != null) {
							done.error(new Exception("" + buffer.remaining() + " byte(s) read after the end of the file"));
							return;
						}
						done.unblock();
						return;
					}
					if (buffer == null) {
						done.error(new Exception("Method readNextBufferAsync returned a null buffer, but this is not the end of the file: offset " + p));
						return;
					}
					int nb = buffer.remaining();
					if (nb == 0) {
						done.error(new Exception("Method readNextBufferAsync returned an empty buffer at offset " + p));
						return;
					}
					int i = 0;
					while (i < nb) {
						int start = (p+i) % testBuf.length;
						int len = nb - i;
						if (len > testBuf.length - start) len = testBuf.length - start;
						for (int j = 0; j < len; ++j) {
							byte b = buffer.get();
							if (b != testBuf[start+j]) {
								done.error(new Exception("Invalid byte " + b + " at offset " + (p + i + start + j) + ", expected is " + testBuf[start+j]));
								return;
							}
						}
						i += len;
					}
					pos.set(p + nb);
	
					read.set(io.readNextBufferAsync(ondone));
				} while (read.get().isDone());
				read.get().onDone(this);
			}
		});
		done.blockThrow(0);
		io.close();
	}
	
	@Test(timeout=240000)
	public void testReadableBufferedNextBuffer() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize(), bufferingSize);
		LinkedList<ByteBuffer> buffers = new LinkedList<>();
		do {
			ByteBuffer buf = io.readNextBuffer();
			if (buf == null) break;
			buffers.add(buf);
		} while (buffers.size() < nbBuf * testBuf.length);
		io.close();
		int pos = 0;
		do {
			ByteBuffer buf = buffers.isEmpty() ? null : buffers.removeFirst();
			if (pos == testBuf.length * nbBuf) {
				if (buf != null)
					throw new Exception("" + buf.remaining() + " byte(s) read after the end of the file");
				break;
			}
			if (buf == null)
				throw new Exception("Method readNextBuffer returned a null buffer, but this is not the end of the file: offset " + pos);
			int nb = buf.remaining();
			if (nb <= 0)
				throw new Exception("Method readNextBuffer returned an empty buffer at offset " + pos);
			int i = 0;
			while (i < nb) {
				int start = (pos+i) % testBuf.length;
				int len = nb - i;
				if (len > testBuf.length - start) len = testBuf.length - start;
				for (int j = 0; j < len; ++j) {
					byte b = buf.get();
					if (b != testBuf[start+j])
						throw new Exception("Invalid byte " + b + " at offset " + (pos + i + start + j) + ", expected is " + testBuf[start+j]);
				}
				i += len;
			}
			pos += nb;
		} while (true);
	}

	@Test(timeout=240000)
	public void testReadableBufferedReadFullySyncIfPossible() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize(), bufferingSize);
		byte[] buf = new byte[testBuf.length];
		Async<Exception> sp = new Async<>();
		new Task.Cpu.FromRunnable("Test readFullySyncIfPossible", Task.PRIORITY_NORMAL, () -> {
			nextSyncIfPossible(io, 0, buf, sp);
		}).start();
		sp.blockThrow(0);
		io.close();
	}
	
	private void nextSyncIfPossible(IO.Readable.Buffered io, int index, byte[] buf, Async<Exception> sp) {
		MutableBoolean ondoneCalled = new MutableBoolean(false);
		Consumer<Pair<Integer, IOException>> ondone = (res) -> {
			ondoneCalled.set(true);
		};
		do {
			ondoneCalled.set(false);
			AsyncSupplier<Integer, IOException> r = io.readFullySyncIfPossible(ByteBuffer.wrap(buf), ondone);
			if (r.isDone()) {
				if (r.hasError()) {
					sp.error(r.getError());
					return;
				}
				if (!ondoneCalled.get()) {
					sp.error(new Exception("ondone not called"));
					return;
				}
				if (index == nbBuf) {
					if (r.getResult().intValue() > 0)
						sp.error(new Exception(r.getResult().intValue() + " byte(s) read after the end"));
					else {
						try {
							AsyncSupplier<Integer, IOException> r2 = io.readFullySyncIfPossible(ByteBuffer.wrap(buf));
							if (r2.blockResult(10000).intValue() > 0)
								sp.error(new Exception("byte(s) read after the end"));
							else
								sp.unblock();
						} catch (Exception e) {
							sp.error(e);
						}
					}
					return;
				}
				if (r.getResult().intValue() != buf.length) {
					sp.error(new Exception("Only " + r.getResult().intValue() + " bytes read on " + buf.length));
					return;
				}
				try {
					Assert.assertArrayEquals(testBuf, buf);
				} catch (Throwable t) {
					sp.error(new Exception(t));
					return;
				}
				index++;
				continue;
			}
			int i = index;
			r.thenStart(new Task.Cpu.FromRunnable("Test readFullySyncIfPossible", Task.PRIORITY_NORMAL, () -> {
				if (!ondoneCalled.get()) {
					sp.error(new Exception("ondone not called"));
					return;
				}
				if (i == nbBuf) {
					if (r.getResult().intValue() > 0)
						sp.error(new Exception(r.getResult().intValue() + " byte(s) read after the end"));
					else {
						try {
							AsyncSupplier<Integer, IOException> r2 = io.readFullySyncIfPossible(ByteBuffer.wrap(buf));
							if (r2.blockResult(10000).intValue() > 0)
								sp.error(new Exception("byte(s) read after the end"));
							else
								sp.unblock();
						} catch (Exception e) {
							sp.error(e);
						}
					}
					return;
				}
				if (r.getResult().intValue() != buf.length) {
					sp.error(new Exception("Only " + r.getResult().intValue() + " bytes read on " + buf.length));
					return;
				}
				try {
					Assert.assertArrayEquals(testBuf, buf);
				} catch (Throwable t) {
					sp.error(new Exception(t));
					return;
				}
				nextSyncIfPossible(io, i + 1, buf, sp);
			}), sp, e -> e);
			return;
		} while (true);
	}
	
}
