package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.util.Pair;

public abstract class TestReadable extends TestIO.UsingGeneratedTestFiles {
	
	protected TestReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract IO.Readable createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		Assume.assumeTrue(nbBuf < 5000);
		return createReadableFromFile(openFile(), getFileSize());
	}
	
	@Test(timeout=120000)
	public void testReadableBySmallBufferSync() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length / 2 + testBuf.length / 3];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		long pos = 0;
		long size = (long)nbBuf * testBuf.length;
		while (pos < size) {
			if (io instanceof IO.PositionKnown)
				Assert.assertEquals(pos, ((IO.PositionKnown)io).getPosition());
			buffer.clear();
			int nb = io.readSync(buffer);
			Assert.assertTrue(nb > 0);
			for (int i = 0; i < nb; ++i) {
				int off = (int)((pos + i) % testBuf.length);
				Assert.assertEquals(testBuf[off], b[i]);
			}
			pos += nb;
		}
		buffer.clear();
		int nb = io.readSync(buffer);
		if (nb > 0)
			throw new Exception("" + nb + " byte(s) read after the end of the file");
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
		io.close();
	}
	
	@Test(timeout=120000)
	public void testReadableBufferByBufferSync() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		for (int i = 0; i < nbBuf; ++i) {
			if (io instanceof IO.PositionKnown)
				Assert.assertEquals(i * testBuf.length, ((IO.PositionKnown)io).getPosition());
			buffer.clear();
			int nb = IOUtil.readFully(io, buffer);
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes read at "+(i*testBuf.length));
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at "+(i*testBuf.length));
		}
		buffer.clear();
		int nb = io.readSync(buffer);
		if (nb > 0)
			throw new Exception("" + nb + " byte(s) read after the end of the file");
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testReadableBufferByBufferFullySync() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		for (int i = 0; i < nbBuf; ++i) {
			if (io instanceof IO.PositionKnown)
				Assert.assertEquals(i * testBuf.length, ((IO.PositionKnown)io).getPosition());
			buffer.clear();
			int nb = io.readFullySync(buffer);
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes read at "+(i*testBuf.length));
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at "+(i*testBuf.length));
		}
		buffer.clear();
		int nb = io.readFullySync(buffer);
		if (nb > 0)
			throw new Exception("" + nb + " byte(s) read after the end of the file");
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
		io.close();
	}

	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testReadableBufferByBufferFullySyncBigBuffer() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length * 1000];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		int done = 0;
		while (done < nbBuf) {
			if (io instanceof IO.PositionKnown)
				Assert.assertEquals(done * testBuf.length, ((IO.PositionKnown)io).getPosition());
			buffer.clear();
			int nb = io.readFullySync(buffer);
			int expected = 1000;
			if (done + expected > nbBuf) expected = nbBuf - done;
			if (nb != testBuf.length * expected)
				throw new Exception("Only "+nb+" bytes read at "+(done*testBuf.length)+", expected was "+(testBuf.length * expected));
			for (int i = 0; i < expected; ++i) {
				if (!ArrayUtil.equals(b, i*testBuf.length, testBuf, 0, testBuf.length))
					throw new Exception("Invalid read at "+((i+done)*testBuf.length));
			}
			done += expected;
		}
		buffer.clear();
		int nb = io.readFullySync(buffer);
		if (nb > 0)
			throw new Exception("" + nb + " byte(s) read after the end of the file");
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
		io.close();
	}

	@Test(timeout=120000)
	public void testReadableBufferByBufferFullyAsync() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		Async<Exception> done = new Async<>();
		MutableInteger i = new MutableInteger(0);
		Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(0, ((IO.PositionKnown)io).getPosition());
		read.set(io.readFullyAsync(buffer, ondone));
		read.get().onDone(new Runnable() {
			@Override
			public void run() {
				do {
					AsyncSupplier<Integer,IOException> res = read.get();
					if (res.isCancelled()) {
						done.error(new Exception("Operation cancelled", res.getCancelEvent()));
						return;
					}
					if (!onDoneBefore.get()) {
						done.error(new Exception("Method readFullyAsync didn't call ondone before listeners on buffer " + i.get()));
						return;
					}
					if (res.hasError()) {
						done.error(new Exception("Error reading buffer " + i.get(), res.getError()));
						return;
					}
					if (i.get() < nbBuf) {
						if (res.getResult().intValue() != testBuf.length) {
							done.error(new Exception("Only "+res.getResult().intValue()+" bytes read at "+(i.get()*testBuf.length)));
							return;
						}
						if (io instanceof IO.PositionKnown) {
							try {
								Assert.assertEquals((i.get() + 1) * testBuf.length, ((IO.PositionKnown)io).getPosition());
							} catch (Throwable t) {
								done.error(new Exception("Error reading position", t));
								return;
							}
						}
					} else {
						if (res.getResult().intValue() > 0) {
							done.error(new Exception("" + res.getResult() + " byte(s) read after the end of the file"));
							return;
						}
						if (io instanceof IO.PositionKnown)
							try {
								Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
							} catch (Throwable t) {
								done.error(new Exception("Error reading position", t));
								return;
							}
						done.unblock();
						return;
					}
					if (!ArrayUtil.equals(b, testBuf)) {
						done.error(new Exception("Invalid read at "+(i.get()*testBuf.length)));
						return;
					}
					i.inc();
					buffer.clear();
					if ((i.get() % 7) == 0) {
						onDoneBefore.set(true);
						read.set(io.readFullyAsync(buffer, null));
					} else {
						onDoneBefore.set(false);
						read.set(io.readFullyAsync(buffer, ondone));
					}
				} while (read.get().isDone());
				read.get().onDone(this);
			}
		});
		done.blockThrow(0);
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(nbBuf * testBuf.length, ((IO.PositionKnown)io).getPosition());
		io.close();
	}

	@Test(timeout=120000)
	public void testReadableBufferByBufferFullyAsyncBigBuffer() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length * 1000];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		Async<Exception> done = new Async<>();
		MutableInteger i = new MutableInteger(0);
		Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
		read.set(io.readFullyAsync(buffer, ondone));
		read.get().onDone(new Runnable() {
			@Override
			public void run() {
				do {
					if (!onDoneBefore.get()) {
						done.error(new Exception("Method readFullyAsync didn't call ondone before listeners"));
						return;
					}
					onDoneBefore.set(false);
					AsyncSupplier<Integer,IOException> res = read.get();
					if (res.hasError()) {
						done.error(res.getError());
						return;
					}
					
					int expected = 1000;
					if (i.get() + expected > nbBuf) expected = nbBuf - i.get();
					if ((expected == 0 && res.getResult().intValue() > 0) || (expected > 0 && res.getResult().intValue() != testBuf.length * expected)) {
						done.error(new Exception("Only "+res.getResult().intValue()+" bytes read at "+(i.get()*testBuf.length)+", expected was "+(testBuf.length * expected)));
						return;
					}
					for (int j = 0; j < expected; ++j) {
						if (!ArrayUtil.equals(b, j*testBuf.length, testBuf, 0, testBuf.length)) {
							done.error(new Exception("Invalid read at "+((j+i.get())*testBuf.length)));
							return;
						}
					}
					i.add(expected);
					
					if (i.get() == nbBuf) {
						done.unblock();
						return;
					}
	
					buffer.clear();
					read.set(io.readFullyAsync(buffer, ondone));
				} while (read.get().isDone());
				read.get().onDone(this);
			}
		});
		done.blockThrow(0);
		io.close();
	}
	
	@Test(timeout=120000)
	public void testReadableBufferByBufferAsync() throws Exception {
		IO.Readable io = createReadableFromFile(openFile(), getFileSize());
		byte[] b = new byte[8192];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		Async<Exception> done = new Async<>();
		MutableInteger pos = new MutableInteger(0);
		Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
		read.set(io.readAsync(buffer, ondone));
		read.get().onDone(new Runnable() {
			@Override
			public void run() {
				do {
					if (!onDoneBefore.get()) {
						done.error(new Exception("Method readAsync didn't call ondone before listeners"));
						return;
					}
					onDoneBefore.set(false);
					AsyncSupplier<Integer,IOException> res = read.get();
					if (res.hasError()) {
						done.error(res.getError());
						return;
					}
					int p = pos.get();
					int nb = res.getResult().intValue();
					if (p == testBuf.length * nbBuf) {
						if (nb > 0) {
							done.error(new Exception("" + nb + " byte(s) read after the end of the file"));
							return;
						}
						done.unblock();
						return;
					}
					int i = 0;
					while (i < nb) {
						int start = (p+i) % testBuf.length;
						int len = nb - i;
						if (len > testBuf.length - start) len = testBuf.length - start;
						for (int j = 0; j < len; ++j)
							if (b[i+j] != testBuf[start+j]) {
								done.error(new Exception("Invalid byte at offset " + (p + i + start + j)));
								return;
							}
						i += len;
					}
					pos.set(p + nb);
					buffer.clear();
					read.set(io.readAsync(buffer, ondone));
				} while (read.get().isDone());
				read.get().onDone(this);
			}
		});
		done.blockThrow(0);
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testReadableSkipSync() throws Exception {
		long size = getFileSize();
		IO.Readable io = createReadableFromFile(openFile(), size);
		long pos = 0;
		byte[] b = new byte[1];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		long toSkip = 1;
		while (pos < size) {
			buffer.clear();
			int nb = io.readFullySync(buffer);
			if (nb != 1)
				throw new Exception("Unexpected end of stream at offset " + pos);
			if (b[0] != testBuf[(int)(pos % testBuf.length)])
				throw new Exception("Invalid byte read at offset " + pos);
			pos++;
			long skipped = io.skipSync(toSkip);
			if (pos + toSkip <= size) {
				if (skipped != toSkip)
					throw new Exception("" + skipped + " byte(s) skipped at position " + pos + ", asked was " + toSkip);
			} else {
				if (skipped != size - pos)
					throw new Exception("" + skipped + " byte(s) skipped at position " + pos + ", asked was " + toSkip + " and file size is " + size);
			}
			pos += skipped;
			toSkip = toSkip + 1 + toSkip/2;
		}
		io.close();
	}

	@Test(timeout=120000)
	public void testReadableSkipAsync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		long size = getFileSize();
		IO.Readable io = createReadableFromFile(openFile(), size);
		MutableLong pos = new MutableLong(0);
		byte[] b = new byte[1];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		MutableLong toSkip = new MutableLong(1);
		Async<Exception> result = new Async<>();
		Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
		Mutable<AsyncSupplier<Long,IOException>> skip = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Long,IOException>> ondone = param -> onDoneBefore.set(true);
		Mutable<Runnable> skipListener = new Mutable<>(null);
		Runnable readListener = new Runnable() {
			@Override
			public void run() {
				if (read.get().hasError()) {
					result.error(read.get().getError());
					return;
				}
				int nb = read.get().getResult().intValue();
				if (nb != 1) {
					result.error(new Exception("Unexpected end of stream at offset " + pos.get()));
					return;
				}
				if (b[0] != testBuf[(int)(pos.get() % testBuf.length)]) {
					result.error(new Exception("Invalid byte read at offset " + pos.get()));
					return;
				}
				pos.inc();
				// kind of random skip, but always the same to reproduce bug if any
				if ((toSkip.get() % 7) == 0)
					skip.set(io.skipAsync(toSkip.get(), null));
				else
					skip.set(io.skipAsync(toSkip.get(), ondone));
				skip.get().onDone(skipListener.get());
			}
		};
		skipListener.set(new Runnable() {
			@Override
			public void run() {
				if (skip.get().hasError()) {
					result.error(skip.get().getError());
					return;
				}
				if (!onDoneBefore.get()) {
					result.error(new Exception("Method skipAsync didn't call ondone before listeners"));
					return;
				}
				onDoneBefore.set(false);
				long skipped = skip.get().getResult().longValue();
				if (pos.get() + toSkip.get() <= size) {
					if (skipped != toSkip.get()) {
						result.error(new Exception("" + skipped + " byte(s) skipped at position " + pos.get() + ", asked was " + toSkip.get()));
						return;
					}
				} else {
					if (skipped != size - pos.get()) {
						result.error(new Exception("" + skipped + " byte(s) skipped at position " + pos.get() + ", but file size is " + size));
						return;
					}
				}
				pos.add(skipped);
				// kind of random skip, but always the same to reproduce bug if any
				toSkip.set(toSkip.get() + 1 + toSkip.get()/2);
				if ((toSkip.get() % 7) == 0)
					onDoneBefore.set(true);
				if (pos.get() >= size) {
					result.unblock();
					return;
				}
				buffer.clear();
				read.set(io.readFullyAsync(buffer));
				read.get().onDone(readListener);
			}
		});

		read.set(io.readFullyAsync(buffer));
		read.get().onDone(readListener);
		
		result.blockThrow(0);
		
		long skipped = io.skipAsync(-(nbBuf + 2) * testBuf.length).blockResult(0).longValue();
		if (io instanceof IO.Readable.Seekable) {
			Assert.assertEquals(-pos.get(), skipped);
		} else {
			Assert.assertEquals(0, skipped);
		}
		
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testSkipSyncNegativeValue() throws Exception {
		Assume.assumeTrue(nbBuf > 2);
		long size = getFileSize();
		IO.Readable io = createReadableFromFile(openFile(), size);
		byte[] b = new byte[1];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		io.skipSync(testBuf.length + testBuf.length / 2);
		long skipped = io.skipSync(-10);
		if (io instanceof IO.Readable.Seekable) {
			if (skipped != -10)
				throw new Exception("Readable.Seekable is supposed to be able to skip with a negative value, skipping -10 bytes returned " + skipped);
		} else {
			if (skipped != 0)
				throw new Exception("Readable is not supposed to be able to skip with a negative value, skipping -10 bytes returned " + skipped + " while 0 was expected.");
		}
		io.readSync(buffer);
		Assert.assertEquals("Invalid byte read after skipSync with negative value", testBuf[testBuf.length / 2 + (int)skipped], b[0]);
		skipped = io.skipSync(-2 * testBuf.length);
		if (io instanceof IO.Readable.Seekable) {
			if (skipped != -((testBuf.length + testBuf.length / 2) - 10 + 1))
				throw new Exception("Skip beyond beginning of IO on Readable.Seekable is supposed to go to the offset 0. Skipping " + (-2 * testBuf.length) + " returned " + skipped + " but expected was " + (-((testBuf.length + testBuf.length / 2) - 10 + 1)));
		} else {
			if (skipped != 0)
				throw new Exception("Readable is not supposed to be able to skip with a negative value, skipping -10 bytes returned " + skipped + " while 0 was expected.");
		}
		io.close();
	}

	/* Should find a better way to test it
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testCanStartReading() throws Exception {
		// first, make the task manager busy
		TaskManager tm = Threading.getDrivesTaskManager().getTaskManager(testFile);
		Object lock = new Object();
		MutableBoolean unlocked = new MutableBoolean(false);
		SynchronizationPoint<NoException> started = new SynchronizationPoint<>();
		MutableBoolean taskBusyDone = new MutableBoolean(false);
		Task.OnFile<Void, NoException> busyTask = new Task.OnFile<Void, NoException>(tm, "test make busy", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				long start = System.currentTimeMillis();
				started.unblock();
				synchronized (lock) {
					while (!unlocked.get()) {
						if (System.currentTimeMillis() - start > 20000)
							return null;
						try { lock.wait(20000); }
						catch (InterruptedException e) {
							// ignore
						}
					}
				}
				taskBusyDone.set(true);
				return null;
			}
		};
		busyTask.start();
		started.block(10000);
		if (!started.isUnblocked())
			throw new Exception("Task to make disk manager busy didn't start after 10 seconds!");
		FileIO.ReadOnly fio = openFile();
		IO.Readable io = createReadableFromFile(fio, getFileSize());
		ISynchronizationPoint<IOException> canStart = io.canStartReading();
		if (canStart.isUnblocked())
			throw new Exception("Can start reading but it is not possible");
		unlocked.set(true);
		synchronized (lock) { lock.notifyAll(); }
		canStart.block(10000);
		if (!canStart.isUnblocked()) {
			if (!taskBusyDone.get())
				throw new Exception("Task to make disk manager busy didn't exit after around 30 seconds!");
			throw new Exception("Cannot start reading after 10 seconds");
		}
		fio.closeAsync();
	}*/
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		if (io instanceof IO.KnownSize) {
			Assert.assertEquals(((long)testBuf.length) * nbBuf, ((IO.KnownSize)io).getSizeSync());
			AsyncSupplier<Long, IOException> getSize = ((IO.KnownSize)io).getSizeAsync();
			getSize.blockException(0);
			Assert.assertEquals(((long)testBuf.length) * nbBuf, getSize.getResult().longValue());
		}
		((IO.Readable)io).canStartReading();
	}

}
