package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteThenReadFullySync() throws Exception {
		T io = openReadWrite();
		ByteBuffer buf = ByteBuffer.wrap(testBuf);
		for (int i = 0; i < nbBuf; ++i) {
			buf.position(0);
			int nb = io.writeSync(buf);
			if (nb != testBuf.length)
				throw new Exception("Write only "+nb+" bytes");
		}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		buf = ByteBuffer.wrap(b);
		for (int i = 0; i < nbBuf; ++i) {
			buf.clear();
			int nb = io.readFullySync(buf);
			if (nb != testBuf.length)
				throw new Exception("Read only "+nb+" bytes at "+i);
			if (!ArrayUtil.equals(b, testBuf)) {
				System.out.println("Invalid read:");
				System.out.println(new String(b, 0, nb));
				throw new Exception("Invalid read at "+i);
			}
		}
		buf.clear();
		if (io.readSync(buf) > 0)
			throw new Exception("More bytes than expected can be read");
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test
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
			// make some pauses to avoid stackoverflow
			if ((i % 10) == 8) {
				prevRead.block(0);
				prevRead = null;
				prevWrite = null;
			}
		}
		if (prevWrite != null) {
			prevWrite.block(0);
			if (prevWrite.hasError()) throw prevWrite.getError();
			if (prevWrite.isCancelled()) throw prevWrite.getCancelEvent();
		}
		while (!buffersToRead.isEmpty()) {
			prevRead = readBuffer(io, prevWrite, prevRead, buffersToRead);
			prevRead.block(0);
			if (prevRead.hasError()) throw prevRead.getError();
			if (prevRead.isCancelled()) throw prevRead.getCancelEvent();
			prevRead = null;
		}
		if (prevRead != null) {
			prevRead.block(0);
			if (prevRead.hasError()) throw prevRead.getError();
			if (prevRead.isCancelled()) throw prevRead.getCancelEvent();
		}
		io.close();
	}
	
	private <T extends IO.Readable.Seekable & IO.Writable.Seekable>
	ISynchronizationPoint<IOException> writeBuffer(T io, ISynchronizationPoint<IOException> prevWrite, List<Integer> buffersToWrite, List<Integer> buffersToRead) {
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
					buffersToRead.add(Integer.valueOf(index));
					index = buffersToWrite.remove(index).intValue();
				}
				MutableBoolean ondoneCalled = new MutableBoolean(false);
				AsyncWork<Integer, IOException> write = io.writeAsync(index * testBuf.length, ByteBuffer.wrap(testBuf), new RunnableWithParameter<Pair<Integer,IOException>>() {
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
					done.unblock();
				}, done);
			}
		};
		if (prevWrite == null) r.run();
		else if (prevWrite.hasError()) return prevWrite;
		else prevWrite.listenInline(r);
		return done;
	}

	private <T extends IO.Readable.Seekable & IO.Writable.Seekable>
	ISynchronizationPoint<IOException> readBuffer(T io, ISynchronizationPoint<IOException> prevWrite, ISynchronizationPoint<IOException> prevRead, List<Integer> buffersToRead) {
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
				AsyncWork<Integer, IOException> read = io.readFullyAsync(pos, ByteBuffer.wrap(b), new RunnableWithParameter<Pair<Integer,IOException>>() {
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
							done.error(new IOException("Invalid data read at " + pos));
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
	@Test
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
				throw new Exception("Read only "+nb+" bytes at buffer "+i);
			if (!ArrayUtil.equals(b, testBuf)) {
				System.out.println("Invalid read:");
				System.out.println(new String(b, 0, nb));
				throw new Exception("Invalid read at buffer "+i);
			}
		}
		io.close();
	}
	
	// TODO test with some seeking operations, sync and async, etc...
	// TODO flush
	
}
