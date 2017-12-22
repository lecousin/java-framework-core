package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

public abstract class TestReadWrite extends TestIO.UsingTestData {

	protected TestReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract <T extends IO.Readable.Seekable & IO.Writable.Seekable> T openReadWrite() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return openReadWrite();
	}

	@SuppressWarnings({ "resource" })
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteThenReadFullySync() throws Exception {
		T io = openReadWrite();
		ByteBuffer buf = ByteBuffer.wrap(testBuf);
		for (int i = 0; i < nbBuf; ++i) {
			buf.position(0);
			int nb = io.writeSync(buf);
			if (nb != testBuf.length)
				throw new Exception("Write only " + nb + " bytes");
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
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testRandomWriteAndReadAsync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		LinkedList<Integer> buffersToWrite = new LinkedList<>();
		LinkedList<Integer> buffersToRead = new LinkedList<>();
		for (int i = 0; i < nbBuf; ++i) buffersToWrite.add(Integer.valueOf(i));
		ISynchronizationPoint<IOException> prevWrite = null;
		ISynchronizationPoint<IOException> prevRead = null;
		for (int i = 0; i < nbBuf; i += 2) {
			prevWrite = writeBuffer(io, prevWrite, buffersToWrite, buffersToRead);
			if (i < nbBuf - 1)
				prevWrite = writeBuffer(io, prevWrite, buffersToWrite, buffersToRead);
			prevRead = readBuffer(io, prevWrite, prevRead, buffersToRead);
			// make some pauses to avoid stack overflow
			if ((i % 10) == 8) {
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
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable> ISynchronizationPoint<IOException> writeBuffer(
		T io, ISynchronizationPoint<IOException> prevWrite, List<Integer> buffersToWrite, List<Integer> buffersToRead
	) {
		SynchronizationPoint<IOException> done = new SynchronizationPoint<>();
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
				AsyncWork<Integer, IOException> write = io.writeAsync(index * testBuf.length, ByteBuffer.wrap(testBuf),
				new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						ondoneCalled.set(true);
					}
				});
				write.listenInline(() -> {
					if (!ondoneCalled.get()) {
						done.error(new IOException("ondone not called by writeAsync"));
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
		else prevWrite.listenInline(r);
		return done;
	}

	private <T extends IO.Readable.Seekable & IO.Writable.Seekable> ISynchronizationPoint<IOException> readBuffer(
		T io, ISynchronizationPoint<IOException> prevWrite, ISynchronizationPoint<IOException> prevRead, List<Integer> buffersToRead
	) {
		SynchronizationPoint<IOException> done = new SynchronizationPoint<>();
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
				AsyncWork<Integer, IOException> read = io.readFullyAsync(pos, ByteBuffer.wrap(b),
				new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						ondoneCalled.set(true);
					}
				});
				read.listenInline(new Runnable() {
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
						done.unblock();
					}
				});
			}
		};
		if (prevWrite != null && prevWrite.hasError()) return prevWrite;
		if (prevRead != null && prevRead.hasError()) return prevRead;
		JoinPoint.listenInline(r, prevWrite, prevRead);
		return done;
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testRandomPartialWriteSync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		int nbBuf = this.nbBuf;
		if (nbBuf > 5000) nbBuf = 5000 + rand.nextInt(1000); // limit size because too long
		// write randomly
		FragmentedRangeLong toWrite = new FragmentedRangeLong(new RangeLong(0, nbBuf * testBuf.length));
		while (!toWrite.isEmpty()) {
			int rangeIndex = rand.nextInt(toWrite.size());
			RangeLong range = toWrite.get(rangeIndex);
			int startPos = range.getLength() >= testBuf.length ? rand.nextInt((int)range.getLength()) : 0;
			int bufOffset = (int)((range.min + startPos) % testBuf.length);
			int len = testBuf.length - bufOffset;
			if (range.min + startPos + len - 1 > range.max) len = (int)(range.max - (range.min + startPos) + 1);
			io.writeSync(range.min + startPos, ByteBuffer.wrap(testBuf, bufOffset, len));
			if (range.max == range.min + startPos + len - 1) {
				toWrite.removeRange(range.min + startPos, range.max);
				continue;
			}
			int len2 = testBuf.length;
			if (range.min + startPos + len + len2 - 1 > range.max) len2 = (int)(range.max - (range.min + startPos + len) + 1);
			io.writeSync(range.min + startPos + len, ByteBuffer.wrap(testBuf, 0, len2));
			toWrite.removeRange(range.min + startPos, range.min + startPos + len + len2 - 1);
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
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
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
		io.writeSync(ByteBuffer.wrap(testBuf, testBuf.length / 2, testBuf.length - testBuf.length / 2));
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
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testSeekAsyncWriteOddEvenThenReadReverse() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWrite();
		// make the file have its final size to be able to use SEEK_END
		io.writeSync(nbBuf * testBuf.length - 1, ByteBuffer.wrap(testBuf, 0, 1));
		// write odd buffers
		ISynchronizationPoint<IOException> prev = null;
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
	ISynchronizationPoint<IOException> writeBufferSeekAsync(T io, int bufIndex, int j, ISynchronizationPoint<IOException> prevWrite) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		Task.Cpu<Void, NoException> taskWrite = new Task.Cpu<Void, NoException>("writeBufferSeekAsync write", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				io.writeAsync(ByteBuffer.wrap(testBuf)).listenInline(result);
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
				AsyncWork<Long, IOException> seek;
				if ((j % 3) == 0)
					seek = io.seekAsync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length);
				else if ((j % 3) == 1) {
					try { seek = io.seekAsync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition()); }
					catch (IOException e) { result.error(e); return null; }
				} else
					seek = io.seekAsync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length);
				seek.listenInline(() -> {
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
	ISynchronizationPoint<IOException> readReverseSeekAsync(T io, int bufIndex, ISynchronizationPoint<IOException> prevOp) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		Task.Cpu<Void, NoException> taskRead = new Task.Cpu<Void, NoException>("readReverseSeekAsync read", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				byte[] b = new byte[testBuf.length];
				if (bufIndex == 0) {
					int ii = 0;
					ii = ii + 1;
				}
				AsyncWork<Integer, IOException> read = io.readFullyAsync(ByteBuffer.wrap(b));
				read.listenInline(() -> {
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
				AsyncWork<Long, IOException> seek;
				if ((bufIndex % 3) == 0)
					seek = io.seekAsync(SeekType.FROM_BEGINNING, bufIndex * testBuf.length);
				else if ((bufIndex % 3) == 1) {
					try { seek = io.seekAsync(SeekType.FROM_CURRENT, bufIndex * testBuf.length - io.getPosition()); }
					catch (IOException e) { result.error(e); return null; }
				} else
					seek = io.seekAsync(SeekType.FROM_END, (nbBuf - bufIndex) * testBuf.length);
				seek.listenInline(() -> {
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
