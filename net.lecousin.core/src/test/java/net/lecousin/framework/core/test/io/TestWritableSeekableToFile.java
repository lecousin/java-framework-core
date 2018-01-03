package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

public abstract class TestWritableSeekableToFile extends TestIO.UsingTestData {

	protected TestWritableSeekableToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	protected abstract <T extends IO.Writable.Seekable & IO.Resizable> T createWritableSeekableFromFile(File file) throws IOException;
	
	@SuppressWarnings("unused")
	protected void flush(IO.Writable.Seekable io) throws Exception {
	}
	
	protected static File createFile() throws IOException {
		File file = File.createTempFile("test", "writableseekable");
		file.deleteOnExit();
		return file;
	}
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createWritableSeekableFromFile(createFile());
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteSyncRandomly() throws Exception {
		File file = createFile();
		IO.Writable.Seekable io = createWritableSeekableFromFile(file);
		((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);

		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		while (!offsets.isEmpty()) {
			int i = rand.nextInt(offsets.size());
			Integer offset = offsets.remove(i);
			int nb = io.writeSync(offset.intValue()*testBuf.length, ByteBuffer.wrap(testBuf));
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes written at "+(offset.intValue()*testBuf.length));
		}
		
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteSyncRandomlyWithSeek() throws Exception {
		File file = createFile();
		IO.Writable.Seekable io = createWritableSeekableFromFile(file);
		((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);

		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		while (!offsets.isEmpty()) {
			int i = rand.nextInt(offsets.size());
			Integer offset = offsets.remove(i);
			if ((offset.intValue() % 3) == 0)
				io.seekSync(SeekType.FROM_BEGINNING, offset.intValue()*testBuf.length);
			else if ((offset.intValue() % 3) == 1)
				io.seekSync(SeekType.FROM_CURRENT, offset.intValue()*testBuf.length - io.getPosition());
			else
				io.seekSync(SeekType.FROM_END, nbBuf*testBuf.length - offset.intValue()*testBuf.length);
			Assert.assertEquals(offset.intValue()*testBuf.length, io.getPosition());
			int nb = io.writeSync(ByteBuffer.wrap(testBuf));
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes written at "+(offset.intValue()*testBuf.length));
			Assert.assertEquals((offset.intValue() + 1)*testBuf.length, io.getPosition());
		}
		
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf);
	}
	
	@Test
	public void testWriteAsyncRandomly() throws Exception {
		File file = createFile();
		IO.Writable.Seekable io = createWritableSeekableFromFile(file);
		((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
		
		LinkedArrayList<Integer> offsets = new LinkedArrayList<Integer>(20);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		RunnableWithParameter<Pair<Integer,IOException>> ondone = new RunnableWithParameter<Pair<Integer,IOException>>() {
			@Override
			public void run(Pair<Integer, IOException> param) {
				onDoneBefore.set(true);
			}
		};
		
		Mutable<AsyncWork<Integer,IOException>> write = new Mutable<>(null);
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				do {
					if (write.get().hasError()) {
						sp.error(write.get().getError());
						return;
					}
					if (write.get().isCancelled()) {
						sp.cancel(write.get().getCancelEvent());
						return;
					}
					if (!onDoneBefore.get()) {
						sp.error(new Exception("Method writeAsync didn't call ondone before listeners"));
						return;
					}
					onDoneBefore.set(false);
					
					if (write.get().getResult().intValue() != testBuf.length) {
						sp.error(new Exception("Unexpected number of bytes written at " + (offset.get()*testBuf.length) + " (" + write.get().getResult().intValue() + "/" + testBuf.length + " bytes read)"));
						return;
					}
	
					if (offsets.isEmpty()) {
						sp.unblock();
						return;
					}
					offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
					write.set(io.writeAsync(offset.get()*testBuf.length, ByteBuffer.wrap(testBuf), ondone));
				} while (write.get().isUnblocked());
				write.get().listenInline(this);
			}
		};
		
		write.set(io.writeAsync(offset.get()*testBuf.length, ByteBuffer.wrap(testBuf), ondone));
		write.get().listenInline(listener);
		
		sp.blockThrow(0);
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf);
	}

	@Test
	public void testWriteAsyncRandomlyWithSeek() throws Exception {
		File file = createFile();
		IO.Writable.Seekable io = createWritableSeekableFromFile(file);
		((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
		
		LinkedArrayList<Integer> offsets = new LinkedArrayList<Integer>(20);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBeforeSeek = new MutableBoolean(false);
		RunnableWithParameter<Pair<Long,IOException>> ondoneseek = new RunnableWithParameter<Pair<Long,IOException>>() {
			@Override
			public void run(Pair<Long, IOException> param) {
				onDoneBeforeSeek.set(true);
			}
		};
		
		Mutable<AsyncWork<Long,IOException>> seek = new Mutable<>(null);
		Mutable<AsyncWork<Integer,IOException>> write = new Mutable<>(null);
		
		Mutable<Runnable> listenerWrite = new Mutable<>(null);
		
		Runnable listenerSeek = new Runnable() {
			@Override
			public void run() {
				if (seek.get().hasError()) {
					sp.error(seek.get().getError());
					return;
				}
				if (seek.get().isCancelled()) {
					sp.cancel(seek.get().getCancelEvent());
					return;
				}
				try {
					if (io.getPosition() != offset.get()*testBuf.length)
						throw new IOException("Invalid position after seek: " + io.getPosition() + ", expected was " + offset.get()*testBuf.length);
				} catch (IOException e) {
					sp.error(e);
					return;
				}
				
				write.set(io.writeAsync(ByteBuffer.wrap(testBuf)));
				write.get().listenInline(listenerWrite.get());
			}
		};
		
		listenerWrite.set(new Runnable() {
			@Override
			public void run() {
				if (write.get().hasError()) {
					sp.error(write.get().getError());
					return;
				}
				if (write.get().isCancelled()) {
					sp.cancel(write.get().getCancelEvent());
					return;
				}
				if (write.get().getResult().intValue() != testBuf.length) {
					sp.error(new IOException("Unexpected number of bytes written: " + write.get().getResult().intValue()));
					return;
				}

				if (offsets.isEmpty()) {
					sp.unblock();
					return;
				}
				offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
				
				if ((offset.get() % 3) == 0)
					seek.set(io.seekAsync(SeekType.FROM_BEGINNING, offset.get()*testBuf.length, ondoneseek));
				else if ((offset.get() % 3) == 1)
					try { seek.set(io.seekAsync(SeekType.FROM_CURRENT, offset.get()*testBuf.length - io.getPosition())); }
					catch (IOException e) { sp.error(e); return; }
				else
					seek.set(io.seekAsync(SeekType.FROM_END, nbBuf*testBuf.length - offset.get()*testBuf.length));
				
				seek.get().listenInline(listenerSeek);
			}
		});
		
		seek.set(io.seekAsync(SeekType.FROM_BEGINNING, offset.get()*testBuf.length, ondoneseek));
		seek.get().listenInline(listenerSeek);
			
		sp.blockThrow(0);
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf);
	}	
	
}
