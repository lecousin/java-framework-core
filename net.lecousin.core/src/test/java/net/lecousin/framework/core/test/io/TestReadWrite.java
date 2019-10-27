package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.mutable.MutableBoolean;

public abstract class TestReadWrite extends TestIO.UsingTestData {

	protected TestReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract <T extends IO.Readable.Seekable & IO.Writable.Seekable> T openReadWrite() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		Assume.assumeTrue(nbBuf < 5000);
		return openReadWrite();
	}

	@SuppressWarnings({ "resource" })
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteThenReadFullySync() throws Exception {
		T io = openReadWrite();
		ByteBuffer buf = ByteBuffer.wrap(testBuf);
		for (int i = 0; i < nbBuf; ++i) {
			buf.position(0);
			int nb = io.writeSync(buf);
			if (nb != testBuf.length)
				throw new Exception("Write only " + nb + " bytes for buffer " + i);
			if (buf.remaining() > 0)
				throw new Exception("Buffer not fully consumed by write operation");
		}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		buf = ByteBuffer.wrap(b);
		for (int i = 0; i < nbBuf; ++i) {
			buf.clear();
			int nb = io.readFullySync(buf);
			if (nb != testBuf.length)
				throw new Exception("Read only " + nb + " bytes at buffer " + i);
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at buffer " + i + ":\r\nRead is:\r\n" + new String(b)
					+ "\r\nExpected is:\r\n" + new String(testBuf));
		}
		buf.clear();
		if (io.readSync(buf) > 0)
			throw new Exception("More bytes than expected can be read");
		io.close();
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testRandomWriteAndReadAsync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		int nbBuf = this.nbBuf;
		if (nbBuf > 25000) nbBuf = 25000; // make test faster
		T io = openReadWrite();
		Assert.assertEquals(0, io.getPosition());
		LinkedList<Integer> buffersToWrite = new LinkedList<>();
		LinkedList<Integer> buffersToRead = new LinkedList<>();
		for (int i = 0; i < nbBuf; ++i) buffersToWrite.add(Integer.valueOf(i));
		IAsync<IOException> prevWrite = null;
		IAsync<IOException> prevRead = null;
		for (int i = 0; i < nbBuf; i += 2) {
			prevWrite = writeBuffer(io, prevWrite, buffersToWrite, buffersToRead);
			if (i < nbBuf - 1)
				prevWrite = writeBuffer(io, prevWrite, buffersToWrite, buffersToRead);
			prevRead = readBuffer(io, prevWrite, prevRead, buffersToRead);
			// make some pauses to avoid stack overflow
			if ((i % 17) == 8) {
				prevRead.blockThrow(0);
				prevRead = null;
				prevWrite = null;
			}
		}
		if (prevWrite != null) {
			prevWrite.blockThrow(0);
		}
		while (!buffersToRead.isEmpty()) {
			prevRead = readBuffer(io, prevWrite, prevRead, buffersToRead);
			prevRead.blockThrow(0);
			prevRead = null;
		}
		if (prevRead != null) {
			prevRead.blockThrow(0);
		}
		io.close();
	}
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable> IAsync<IOException> writeBuffer(
		T io, IAsync<IOException> prevWrite, List<Integer> buffersToWrite, List<Integer> buffersToRead
	) {
		Async<IOException> done = new Async<>();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (prevWrite != null && prevWrite.hasError()) {
					done.error(prevWrite.getError());
					return;
				}
				if (prevWrite != null && prevWrite.isCancelled()) {
					done.cancel(prevWrite.getCancelEvent());
					return;
				}
				int index;
				synchronized (buffersToRead) {
					index = rand.nextInt(buffersToWrite.size());
					index = buffersToWrite.remove(index).intValue();
				}
				MutableBoolean ondoneCalled = new MutableBoolean(false);
				int bufIndex = index;
				ByteBuffer buf = ByteBuffer.wrap(testBuf);
				AsyncSupplier<Integer, IOException> write = io.writeAsync(index * testBuf.length, buf, param -> ondoneCalled.set(true));
				write.onDone(() -> {
					if (!ondoneCalled.get()) {
						done.error(new IOException("ondone not called by writeAsync"));
						return;
					}
					if (buf.remaining() > 0) {
						done.error(new IOException("Buffer not fully consumed by write operation"));
						return;
					}
					try {
						Assert.assertEquals("Write at a given position should not change the IO cursor", 0, io.getPosition());
					} catch (Throwable t) {
						done.error(new IOException(t));
						return;
					}
					synchronized (buffersToRead) {
						buffersToRead.add(Integer.valueOf(bufIndex));
					}
					done.unblock();
				}, done);
			}
		};
		if (prevWrite == null) r.run();
		else if (prevWrite.hasError()) return prevWrite;
		else prevWrite.onDone(r);
		return done;
	}

	private <T extends IO.Readable.Seekable & IO.Writable.Seekable> IAsync<IOException> readBuffer(
		T io, IAsync<IOException> prevWrite, IAsync<IOException> prevRead, List<Integer> buffersToRead
	) {
		Async<IOException> done = new Async<>();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (prevWrite != null && prevWrite.hasError()) {
					done.error(prevWrite.getError());
					return;
				}
				if (prevWrite != null && prevWrite.isCancelled()) {
					done.cancel(prevWrite.getCancelEvent());
					return;
				}
				if (prevRead != null && prevRead.hasError()) {
					done.error(prevRead.getError());
					return;
				}
				if (prevRead != null && prevRead.isCancelled()) {
					done.cancel(prevRead.getCancelEvent());
					return;
				}
				int index;
				synchronized (buffersToRead) {
					if (buffersToRead.isEmpty()) {
						done.unblock();
						return;
					}
					index = rand.nextInt(buffersToRead.size());
					index = buffersToRead.remove(index).intValue();
				}
				byte[] b = new byte[testBuf.length];
				long pos = index * testBuf.length;
				MutableBoolean ondoneCalled = new MutableBoolean(false);
				AsyncSupplier<Integer, IOException> read = io.readFullyAsync(pos, ByteBuffer.wrap(b), param -> ondoneCalled.set(true));
				read.onDone(new Runnable() {
					@Override
					public void run() {
						if (read.hasError()) {
							done.error(read.getError());
							return;
						}
						if (!ondoneCalled.get()) {
							done.error(new IOException("ondone not called by readFullyAsync"));
							return;
						}
						if (read.getResult().intValue() != testBuf.length) {
							done.error(new IOException("Only " + read.getResult().intValue() + " byte(s) read at " + pos
								+ ", expected was " + testBuf.length));
							return;
						}
						if (!ArrayUtil.equals(testBuf, b)) {
							done.error(new IOException("Invalid data read at " + pos + ":\r\nRead is:\r\n" + new String(b)
								+ "\r\nExpected is:\r\n" + new String(testBuf)));
							return;
						}
						try {
							Assert.assertEquals("Read at a given position should not change the IO cursor", 0, io.getPosition());
						} catch (Throwable t) {
							done.error(new IOException(t));
							return;
						}
						done.unblock();
					}
				});
			}
		};
		if (prevWrite != null && prevWrite.hasError()) return prevWrite;
		if (prevRead != null && prevRead.hasError()) return prevRead;
		JoinPoint.joinThenDo(r, prevWrite, prevRead);
		return done;
	}
	
	@SuppressWarnings("resource")
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testRandomPartialWriteSync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		Assert.assertEquals(0, io.getPosition());
		int nbBuf = this.nbBuf;
		if (nbBuf > 5000) nbBuf = 5000 + rand.nextInt(1000); // limit size because too long
		// write randomly
		FragmentedRangeLong toWrite = new FragmentedRangeLong(new RangeLong(0, nbBuf * testBuf.length - 1));
		while (!toWrite.isEmpty()) {
			int rangeIndex = rand.nextInt(toWrite.size());
			RangeLong range = toWrite.get(rangeIndex);
			int startPos = range.getLength() >= testBuf.length ? rand.nextInt((int)range.getLength()) : 0;
			int bufOffset = (int)((range.min + startPos) % testBuf.length);
			int len = testBuf.length - bufOffset;
			if (range.min + startPos + len - 1 > range.max) len = (int)(range.max - (range.min + startPos) + 1);
			ByteBuffer buf = ByteBuffer.wrap(testBuf, bufOffset, len);
			try {
				io.writeSync(range.min + startPos, buf);
			} catch (Exception e) {
				throw new Exception("Error writing at " + (range.min + startPos) + ", " + len + " bytes", e);
			}
			Assert.assertEquals("Remaining data not written at position " + (range.min + startPos) + " on " + len, 0, buf.remaining());
			Assert.assertEquals("Write at a given position should not change the IO cursor", 0, io.getPosition());
			if (range.max == range.min + startPos + len - 1) {
				toWrite.removeRange(range.min + startPos, range.max);
				continue;
			}
			int len2 = testBuf.length;
			if (range.min + startPos + len + len2 - 1 > range.max) len2 = (int)(range.max - (range.min + startPos + len) + 1);
			io.writeSync(range.min + startPos + len, ByteBuffer.wrap(testBuf, 0, len2));
			toWrite.removeRange(range.min + startPos, range.min + startPos + len + len2 - 1);
			Assert.assertEquals("Write at a given position should not change the IO cursor", 0, io.getPosition());
		}
		// read
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = io.readFullySync(ByteBuffer.wrap(b));
			if (nb != testBuf.length)
				throw new Exception("Read only " + nb + " bytes at buffer " + i);
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at buffer " + i + ":\r\nRead is:\r\n" + new String(b)
					+ "\r\nExpected is:\r\n" + new String(testBuf));
		}
		io.close();
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteSyncOneShot() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		byte[] bigBuffer = new byte[nbBuf * testBuf.length];
		for (int i = 0; i < nbBuf; ++i)
			System.arraycopy(testBuf, 0, bigBuffer, i * testBuf.length, testBuf.length);
		io.writeSync(0, ByteBuffer.wrap(bigBuffer));
		// read
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = io.readFullySync(ByteBuffer.wrap(b));
			if (nb != testBuf.length)
				throw new Exception("Read only " + nb + " bytes at buffer " + i);
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at buffer " + i + ":\r\nRead is:\r\n" + new String(b)
					+ "\r\nExpected is:\r\n" + new String(testBuf));
		}
		io.close();
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteAsyncOneShot() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		byte[] bigBuffer = new byte[nbBuf * testBuf.length];
		for (int i = 0; i < nbBuf; ++i)
			System.arraycopy(testBuf, 0, bigBuffer, i * testBuf.length, testBuf.length);
		Assert.assertEquals(bigBuffer.length, io.writeAsync(0, ByteBuffer.wrap(bigBuffer)).blockResult(60000).intValue());
		// read
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = io.readFullySync(ByteBuffer.wrap(b));
			if (nb != testBuf.length)
				throw new Exception("Read only " + nb + " bytes at buffer " + i);
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at buffer " + i + ":\r\nRead is:\r\n" + new String(b)
					+ "\r\nExpected is:\r\n" + new String(testBuf));
		}
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testDichotomicWriteSeekSyncThenReverseReadSeekSync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		// make the file have its final size to be able to use SEEK_END
		io.writeSync(nbBuf * testBuf.length - 1, ByteBuffer.wrap(testBuf, 0, 1));
		// write
		dichotomicWriteSync(io, 0, nbBuf - 1, SeekType.FROM_BEGINNING);
		// read
		byte[] b = new byte[testBuf.length];
		for (int bufIndex = nbBuf - 1; bufIndex >= 0; bufIndex--) {
			if ((bufIndex % 3) == 0)
				io.seekSync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length);
			else if ((bufIndex % 3) == 1)
				io.seekSync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition());
			else
				io.seekSync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length);
			int nb = io.readFullySync(ByteBuffer.wrap(b));
			if (nb != testBuf.length)
				throw new AssertionError("Only " + nb + " byte(s) read at buffer " + bufIndex);
			if (!ArrayUtil.equals(testBuf, b))
				throw new AssertionError("Invalid read at buffer " + bufIndex + ":\r\nRead is:\r\n" + new String(b)
						+ "\r\nExpected is:\r\n" + new String(testBuf));
		}
		io.close();
	}
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable>
	void dichotomicWriteSync(T io, int bufStart, int bufEnd, SeekType seekType) throws IOException {
		int bufIndex = bufStart + (bufEnd - bufStart) / 2;
		// write the second half of the middle buffer
		switch (seekType) {
		default:
		case FROM_BEGINNING: io.seekSync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length + testBuf.length / 2); break;
		case FROM_END: io.seekSync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length - testBuf.length / 2); break;
		case FROM_CURRENT: io.seekSync(SeekType.FROM_CURRENT, bufIndex * testBuf.length + testBuf.length / 2 - io.getPosition()); break;
		}
		ByteBuffer b = ByteBuffer.wrap(testBuf, testBuf.length / 2, testBuf.length - testBuf.length / 2);
		io.writeSync(b);
		Assert.assertEquals(0, b.remaining());
		b = null;
		// write before
		if (bufIndex > bufStart)
			dichotomicWriteSync(io, bufStart, bufIndex - 1, 
				(bufIndex % 3 == 0) ? SeekType.FROM_BEGINNING :
				(bufIndex % 3 == 1) ? SeekType.FROM_CURRENT : SeekType.FROM_END);
		// write after
		if (bufIndex < bufEnd)
			dichotomicWriteSync(io, bufIndex + 1, bufEnd,
				(bufIndex % 3 == 0) ? SeekType.FROM_CURRENT :
				(bufIndex % 3 == 1) ? SeekType.FROM_END : SeekType.FROM_BEGINNING);
		// write the first half of the middle buffer
		switch (seekType) {
		default:
		case FROM_BEGINNING: io.seekSync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length); break;
		case FROM_END: io.seekSync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length); break;
		case FROM_CURRENT: io.seekSync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition()); break;
		}
		io.writeSync(ByteBuffer.wrap(testBuf, 0, testBuf.length / 2));
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testSeekAsyncWriteOddEvenThenReadReverse() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		// make the file have its final size to be able to use SEEK_END
		io.writeSync(nbBuf * testBuf.length - 1, ByteBuffer.wrap(testBuf, 0, 1));
		// write odd buffers
		IAsync<IOException> prev = null;
		int index = 0;
		for (int i = 1; i < nbBuf; i += 2)
			prev = writeBufferSeekAsync(io, i, index++, prev);
		// write even buffers
		for (int i = 0; i < nbBuf; i += 2)
			prev = writeBufferSeekAsync(io, i, index++, prev);
		// read reverse
		for (int i = nbBuf - 1; i >= 0; --i)
			prev = readReverseSeekAsync(io, i, prev);
		prev.blockThrow(0);
		io.close();
	}
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable>
	IAsync<IOException> writeBufferSeekAsync(T io, int bufIndex, int j, IAsync<IOException> prevWrite) {
		Async<IOException> result = new Async<>();
		Task.Cpu<Void, NoException> taskWrite = new Task.Cpu<Void, NoException>("writeBufferSeekAsync write", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				io.writeAsync(ByteBuffer.wrap(testBuf)).onDone(result);
				return null;
			}
		};
		Task.Cpu<Void, NoException> taskSeek = new Task.Cpu<Void, NoException>("writeBufferSeekAsync seek", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				if (prevWrite != null) {
					if (prevWrite.hasError()) {
						result.error(prevWrite.getError());
						return null;
					}
					if (prevWrite.isCancelled()) {
						result.cancel(prevWrite.getCancelEvent());
						return null;
					}
				}
				AsyncSupplier<Long, IOException> seek;
				if ((j % 3) == 0)
					seek = io.seekAsync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length);
				else if ((j % 3) == 1) {
					try { seek = io.seekAsync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition()); }
					catch (IOException e) { result.error(e); return null; }
				} else
					seek = io.seekAsync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length);
				seek.onDone(() -> {
					if (seek.hasError()) result.error(seek.getError());
					else if (seek.isCancelled()) result.cancel(seek.getCancelEvent());
					else taskWrite.start();
				});
				return null;
			}
		};
		if (prevWrite == null) taskSeek.start();
		else taskSeek.startOn(prevWrite, true);
		return result;
	}
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable>
	IAsync<IOException> readReverseSeekAsync(T io, int bufIndex, IAsync<IOException> prevOp) {
		Async<IOException> result = new Async<>();
		Task.Cpu<Void, NoException> taskRead = new Task.Cpu<Void, NoException>("readReverseSeekAsync read", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				byte[] b = new byte[testBuf.length];
				if (bufIndex == 0) {
					int ii = 0;
					ii = ii + 1;
				}
				AsyncSupplier<Integer, IOException> read = io.readFullyAsync(ByteBuffer.wrap(b));
				read.onDone(() -> {
					if (read.hasError()) result.error(read.getError());
					else if (read.isCancelled()) result.cancel(read.getCancelEvent());
					else if (read.getResult().intValue() != testBuf.length)
						result.error(new IOException(
							"Only " + read.getResult().intValue() + " byte(s) read at buffer " + bufIndex
						));
					else if (!ArrayUtil.equals(b, testBuf))
						result.error(new IOException(
							"Invalid read at buffer " + bufIndex + ":\r\nRead is:\r\n" + new String(b)
							+ "\r\nExpected is:\r\n" + new String(testBuf)));
					else
						result.unblock();
				});
				return null;
			}
		};
		Task.Cpu<Void, NoException> taskSeek = new Task.Cpu<Void, NoException>("readReverseSeekAsync seek", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				if (prevOp != null) {
					if (prevOp.hasError()) {
						result.error(prevOp.getError());
						return null;
					}
					if (prevOp.isCancelled()) {
						result.cancel(prevOp.getCancelEvent());
						return null;
					}
				}
				AsyncSupplier<Long, IOException> seek;
				if ((bufIndex % 3) == 0)
					seek = io.seekAsync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length);
				else if ((bufIndex % 3) == 1) {
					try { seek = io.seekAsync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition()); }
					catch (IOException e) { result.error(e); return null; }
				} else
					seek = io.seekAsync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length);
				seek.onDone(() -> {
					if (seek.hasError()) result.error(seek.getError());
					else if (seek.isCancelled()) result.cancel(seek.getCancelEvent());
					else taskRead.start();
				});
				return null;
			}
		};
		if (prevOp == null) taskSeek.start();
		else taskSeek.startOn(prevOp, true);
		return result;
	}
	
}
