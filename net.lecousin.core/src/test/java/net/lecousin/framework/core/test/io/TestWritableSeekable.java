package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Assume;
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

public abstract class TestWritableSeekable extends TestWritable {

	protected TestWritableSeekable(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	protected abstract IO.Writable.Seekable createWritableSeekable() throws IOException;
	
	@Override
	protected IO.Writable createWritable() throws IOException {
		return createWritableSeekable();
	}
	
	@SuppressWarnings("unused")
	protected void flush(IO.Writable.Seekable io) throws Exception {
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteSyncRandomly() throws Exception {
		IO.Writable.Seekable io = createWritableSeekable();
		Assert.assertEquals(0, io.getPosition());
		if (io instanceof IO.Resizable) {
			((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
			Assert.assertEquals(0, io.getPosition());
		}

		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		while (!offsets.isEmpty()) {
			int i = rand.nextInt(offsets.size());
			Integer offset = offsets.remove(i);
			int nb = io.writeSync(offset.intValue()*testBuf.length, ByteBuffer.wrap(testBuf));
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes written at "+(offset.intValue()*testBuf.length));
			Assert.assertEquals("Write at a given position should not change the IO cursor", 0, io.getPosition());
		}
		
		flush(io);
		io.close();
		check();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteSyncRandomlyWithSeek() throws Exception {
		IO.Writable.Seekable io = createWritableSeekable();
		if (io instanceof IO.Resizable) {
			((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
		}

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
		check();
	}
	
	@Test
	public void testWriteAsyncRandomly() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		IO.Writable.Seekable io = createWritableSeekable();
		Assert.assertEquals(0, io.getPosition());
		if (io instanceof IO.Resizable) {
			((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
			Assert.assertEquals(0, io.getPosition());
		}
		
		LinkedArrayList<Integer> offsets = new LinkedArrayList<Integer>(20);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
		
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
					try {
						Assert.assertEquals("Write at a given position should not change the IO cursor", 0, io.getPosition());
					} catch (Throwable t) {
						sp.error(new Exception(t));
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
		check();
	}

	@Test
	public void testWriteAsyncRandomlyWithSeek() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		IO.Writable.Seekable io = createWritableSeekable();
		if (io instanceof IO.Resizable) {
			((IO.Resizable)io).setSizeSync(nbBuf * testBuf.length);
		}
		
		LinkedArrayList<Integer> offsets = new LinkedArrayList<Integer>(20);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBeforeSeek = new MutableBoolean(false);
		Consumer<Pair<Long,IOException>> ondoneseek = param -> onDoneBeforeSeek.set(true);
		
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
		check();
	}
	
}
