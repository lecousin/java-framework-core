package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.ReadableByteStream;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestReadableBuffered extends TestReadableByteStream {
	
	protected TestReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;

	@Override
	protected ReadableByteStream createReadableByteStreamFromFile(ReadOnly file, long fileSize) throws Exception {
		return createReadableBufferedFromFile(file, fileSize);
	}
	
	@Test(timeout=120000)
	public void testReadableBufferedByteByByteAsync() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize());
		MutableInteger i = new MutableInteger(0);
		MutableInteger j = new MutableInteger(0);
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
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
						io.canStartReading().listenAsync(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), true);
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
					io.skipAsync(skipBuf * testBuf.length).listenAsyncSP(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), sp);
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
						io.canStartReading().listenAsync(new Task.Cpu.FromRunnable("Test readAsync", io.getPriority(), this), true);
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
	
	@Test(timeout=120000)
	public void testReadableBufferedNextBufferAsync() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize());
		SynchronizationPoint<Exception> done = new SynchronizationPoint<>();
		MutableInteger pos = new MutableInteger(0);
		Mutable<AsyncWork<ByteBuffer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		RunnableWithParameter<Pair<ByteBuffer,IOException>> ondone = new RunnableWithParameter<Pair<ByteBuffer,IOException>>() {
			@Override
			public void run(Pair<ByteBuffer, IOException> param) {
				onDoneBefore.set(true);
			}
		};
		read.set(io.readNextBufferAsync(ondone));
		read.get().listenInline(new Runnable() {
			@Override
			public void run() {
				do {
					if (!onDoneBefore.get()) {
						done.error(new Exception("Method readNextBufferAsync didn't call ondone before listeners"));
						return;
					}
					onDoneBefore.set(false);
					AsyncWork<ByteBuffer,IOException> res = read.get();
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
				} while (read.get().isUnblocked());
				read.get().listenInline(this);
			}
		});
		done.blockThrow(0);
		io.close();
	}

	@Test(timeout=120000)
	public void testReadableBufferedReadFullySyncIfPossible() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize());
		byte[] buf = new byte[testBuf.length];
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		new Task.Cpu.FromRunnable("Test readFullySyncIfPossible", Task.PRIORITY_NORMAL, () -> {
			nextSyncIfPossible(io, 0, buf, sp);
		}).start();
		sp.blockThrow(0);
		io.close();
	}
	
	private void nextSyncIfPossible(IO.Readable.Buffered io, int index, byte[] buf, SynchronizationPoint<Exception> sp) {
		MutableBoolean ondoneCalled = new MutableBoolean(false);
		RunnableWithParameter<Pair<Integer, IOException>> ondone = (res) -> {
			ondoneCalled.set(true);
		};
		do {
			if (index == nbBuf) {
				sp.unblock();
				return;
			}
			ondoneCalled.set(false);
			AsyncWork<Integer, IOException> r = io.readFullySyncIfPossible(ByteBuffer.wrap(buf), ondone);
			if (r.isUnblocked()) {
				if (r.hasError()) {
					sp.error(r.getError());
					return;
				}
				if (!ondoneCalled.get()) {
					sp.error(new Exception("ondone not called"));
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
			r.listenAsyncSP(new Task.Cpu.FromRunnable("Test readFullySyncIfPossible", Task.PRIORITY_NORMAL, () -> {
				if (!ondoneCalled.get()) {
					sp.error(new Exception("ondone not called"));
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
			}), sp);
			return;
		} while (true);
	}
	
}
