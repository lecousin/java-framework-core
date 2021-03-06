package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestReadableSeekable extends TestIO.UsingGeneratedTestFiles {

	protected TestReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		Assume.assumeTrue(nbBuf < 5000);
		return createReadableSeekableFromFile(openFile(), getFileSize());
	}
	
	@Test
	public void testSeekableByteByByteSync() throws Exception {
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize())) {
			Assert.assertEquals(0, io.getPosition());
			byte[] b = new byte[1];
			ByteBuffer buffer = ByteBuffer.wrap(b);
			LinkedList<Long> offsets = new LinkedList<>();
			for (int i = 0; i < nbBuf && offsets.size() < 2000 && (nbBuf < 1000 || i < nbBuf - 1); ++i) {
				if (i > 100 && rand.nextBoolean()) continue;
				for (int j = 0; j < testBuf.length && offsets.size() < 2000; ++j) {
					if (offsets.size() < 1000 || rand.nextBoolean())
						offsets.add(Long.valueOf(i * testBuf.length + j));
				}
			}
			if (nbBuf >= 1000)
				for (int j = 0; j < testBuf.length && offsets.size() < 2000; ++j)
					offsets.add(Long.valueOf((nbBuf - 1) * testBuf.length + j));
					
			while (!offsets.isEmpty()) {
				int i = rand.nextInt(offsets.size());
				Long offset = offsets.remove(i);
				buffer.clear();
				int nb = io.readSync(offset.longValue(), buffer);
				if (nb <= 0)
					throw new Exception("Unexpected end of stream at " + offset);
				if (nb > 1)
					throw new Exception("Unexpected number of bytes read at " + offset + ": " + nb + " bytes returned, only one requested");
				if (b[0] != testBuf[(int)(offset.longValue() % testBuf.length)])
					throw new Exception("Invalid byte " + (b[0]&0xFF) + " at "+ offset);
				Assert.assertEquals("Read at a given position should not change the IO cursor", 0, io.getPosition());
			}
			buffer.clear();
			if (io.readSync((long)nbBuf * testBuf.length, buffer) > 0)
				throw new Exception("Byte read after the end of the file");
		}
	}

	@Test
	public void testSeekableByteByByteAsync() throws Exception {
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize())) {
			Assert.assertEquals(0, io.getPosition());
			byte[] b = new byte[1];
			ByteBuffer buffer = ByteBuffer.wrap(b);
			ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
			for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
			
			MutableInteger j = new MutableInteger(0);
			MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
			if (nbBuf == 0) j.set(testBuf.length);
			
			Async<Exception> sp = new Async<>();
			
			MutableBoolean onDoneBefore = new MutableBoolean(false);
			Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
			
			Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
			Runnable listener = new Runnable() {
				@Override
				public void run() {
					do {
						if (read.get().hasError()) {
							sp.error(read.get().getError());
							return;
						}
						if (!onDoneBefore.get()) {
							sp.error(new Exception("Method readAsync didn't call ondone before listeners"));
							return;
						}
						
						if (offsets.isEmpty() && j.get() == testBuf.length) {
							if (read.get().getResult().intValue() > 0) {
								sp.error(new Exception("Byte read after the end of the file"));
								return;
							}
							sp.unblock();
							return;
						}
						if (read.get().getResult().intValue() != 1) {
							sp.error(new Exception("Unexpected end of stream at " + (offset.get()*testBuf.length+j.get()) + ": 1 byte expected, " + read.get().getResult().intValue() + " read"));
							return;
						}
						if (b[0] != testBuf[j.get()]) {
							sp.error(new Exception("Invalid byte "+(b[0]&0xFF)+" at "+(offset.get()*testBuf.length+j.get())));
							return;
						}
						try {
							Assert.assertEquals("Read at a given position should not change the IO cursor", 0, io.getPosition());
						} catch (Throwable t) {
							sp.error(new Exception(t));
							return;
						}
		
						if (j.inc() == testBuf.length) {
							if (offsets.isEmpty()) {
								// read again to test we cannot read beyond the end of the file
								offset.set(nbBuf-1);
							} else {
								if (nbBuf > 1000 && (offsets.size() % 100) == 60) {
									// make the test faster
									for (int skip = 0; skip < 130 && offsets.size() > 1; ++skip)
										offsets.remove(rand.nextInt(offsets.size()));
								}
		
								offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
								j.set(0);
							}
						}
		
						buffer.clear();
						if ((j.get() % 7) == 0) {
							onDoneBefore.set(true);
							read.set(io.readAsync(offset.get()*testBuf.length+j.get(), buffer, null));
						} else {
							onDoneBefore.set(false);
							read.set(io.readAsync(offset.get()*testBuf.length+j.get(), buffer, ondone));
						}
					} while (read.get().isDone());
					read.get().onDone(this);
				}
			};
			
			read.set(io.readAsync(offset.get()*testBuf.length+j.get(), buffer, ondone));
			read.get().onDone(listener);
	
			sp.blockThrow(0);
		}		
	}
	
	@Test
	public void testSeekableBufferByBufferFullySync() throws Exception {
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize())) {
			Assert.assertEquals(0, io.getPosition());
			byte[] b = new byte[testBuf.length];
			ByteBuffer buffer = ByteBuffer.wrap(b);
			ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
			for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
			while (!offsets.isEmpty()) {
				int i = rand.nextInt(offsets.size());
				Integer offset = offsets.remove(i);
				buffer.clear();
				int nb = io.readFullySync(offset.intValue()*testBuf.length, buffer);
				if (nb != testBuf.length)
					throw new Exception("Only "+nb+" bytes read at "+(offset.intValue()*testBuf.length));
				if (!ArrayUtil.equals(b, testBuf))
					throw new Exception("Invalid read at "+(offset.intValue()*testBuf.length));
				Assert.assertEquals("Read at a given position should not change the IO cursor", 0, io.getPosition());
			}
			buffer.clear();
			if (io.readFullySync((long)nbBuf * testBuf.length, buffer) > 0)
				throw new Exception("Bytes read after the end of the file");
		}
	}
	
	@Test
	public void testSeekableBufferByBufferFullyAsync() throws Exception {
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize())) {
			Assert.assertEquals(0, io.getPosition());
			Async<Exception> sp = _testSeekableBufferByBufferFullyAsync(io);
			sp.blockThrow(0);
		}
	}

	private Async<Exception> _testSeekableBufferByBufferFullyAsync(IO.Readable.Seekable io) {
		byte[] b = new byte[testBuf.length];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		LinkedArrayList<Integer> offsets = new LinkedArrayList<Integer>(20);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		
		Async<Exception> sp = new Async<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Integer,IOException>> ondone = param -> onDoneBefore.set(true);
		
		Mutable<AsyncSupplier<Integer,IOException>> read = new Mutable<>(null);
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				do {
					if (read.get().hasError()) {
						sp.error(read.get().getError());
						return;
					}
					if (read.get().isCancelled()) {
						sp.cancel(read.get().getCancelEvent());
						return;
					}
					if (!onDoneBefore.get()) {
						sp.error(new Exception("Method readFullyAsync didn't call ondone before listeners"));
						return;
					}
					
					if (offset.get() == nbBuf) {
						if (read.get().getResult().intValue() > 0) {
							sp.error(new Exception("Bytes read after the end of the file"));
							return;
						}
						sp.unblock();
						return;
					}
					if (read.get().getResult().intValue() != testBuf.length) {
						sp.error(new Exception("Unexpected end of stream at " + (offset.get()*testBuf.length) + " (" + read.get().getResult().intValue() + "/" + testBuf.length + " bytes read)"));
						return;
					}
					if (!ArrayUtil.equals(b, testBuf)) {
						sp.error(new Exception("Invalid data read at "+(offset.get()*testBuf.length)));
						return;
					}
					try {
						Assert.assertEquals("Read at a given position should not change the IO cursor", 0, io.getPosition());
					} catch (Throwable t) {
						sp.error(new Exception(t));
						return;
					}
	
					if (offsets.isEmpty()) {
						// read again to test we cannot read beyond the end of the file
						offset.set(nbBuf);
					} else {
						offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
					}
	
					buffer.clear();
					if ((offset.get() % 7) == 0) {
						onDoneBefore.set(true);
						read.set(io.readFullyAsync(offset.get()*testBuf.length, buffer, null));
					} else {
						onDoneBefore.set(false);
						read.set(io.readFullyAsync(offset.get()*testBuf.length, buffer, ondone));
					}
				} while (read.get().isDone());
				read.get().onDone(this);
			}
		};
		
		read.set(io.readFullyAsync(offset.get()*testBuf.length, buffer, ondone));
		read.get().onDone(listener);
		
		return sp;
	}
	

	@Test
	public void testConcurrentAccessToSeekableBufferByBufferFullyAsync() throws Exception {
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize())) {
			Assert.assertEquals(0, io.getPosition());
			int nbConc = Runtime.getRuntime().availableProcessors() * 3;
			JoinPoint<Exception> jp = new JoinPoint<>();
			jp.addToJoin(nbConc);
			for (int t = 0; t < nbConc; ++t) {
				Task.cpu("Test Concurrent access to IO.Readable.Seekable",Task.Priority.NORMAL, task -> {
					Async<Exception> sp = _testSeekableBufferByBufferFullyAsync(io);
					jp.addToJoin(sp);
					jp.joined();
					return null;
				}).start();
			}
			jp.start();
			jp.blockThrow(0);
		}
	}
	
	@Test
	public void testSeekSync() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			testSeekSync(io, SeekType.FROM_BEGINNING, 0, 0, size);
			testSeekSync(io, SeekType.FROM_END, 0, size, size);
			if (size > 234567) {
				testSeekSync(io, SeekType.FROM_BEGINNING, 123456, 123456, size);
				testSeekSync(io, SeekType.FROM_CURRENT, 12345, 123456 + 12345, size);
				testSeekSync(io, SeekType.FROM_END, 234567, size - 234567, size);
				testSeekSync(io, SeekType.FROM_CURRENT, 999, size - 234567 + 999, size);
				testSeekSync(io, SeekType.FROM_CURRENT, -8888, size - 234567 + 999 - 8888, size);
				testSeekSync(io, SeekType.FROM_CURRENT, 0, size - 234567 + 999 - 8888, size);
			} else if (size > 0) {
				testSeekSync(io, SeekType.FROM_BEGINNING, 1, 1, size);
				testSeekSync(io, SeekType.FROM_CURRENT, 9, 10, size);
				testSeekSync(io, SeekType.FROM_CURRENT, -4, 6, size);
				testSeekSync(io, SeekType.FROM_END, -6, size, size);
				testSeekSync(io, SeekType.FROM_END, 6, size-6, size);
				testSeekSync(io, SeekType.FROM_CURRENT, 2, size-4, size);
				testSeekSync(io, SeekType.FROM_BEGINNING, 8, 8, size);
			} else {
				testSeekSync(io, SeekType.FROM_CURRENT, 10, 0, size);
			}
			
			testSeekSync(io, SeekType.FROM_BEGINNING, size + 10, size, size);
			testSeekSync(io, SeekType.FROM_END, size + 10, 0, size);
		}		
	}
	
	private void testSeekSync(IO.Readable.Seekable io, SeekType type, long move, long expectedPosition, long size) throws Exception {
		long p = io.seekSync(type, move);
		if (p != expectedPosition)
			throw new Exception("Invalid seek: returned position " + p + ", expected is " + expectedPosition);
		p = io.getPosition();
		if (p != expectedPosition)
			throw new Exception("getPosition returned an invalid position " + p + ", expected is " + expectedPosition);
		byte[] b = new byte[1];
		int nb = io.readSync(ByteBuffer.wrap(b));
		if (expectedPosition == size) {
			if (nb > 0) throw new Exception("Can read after file");
		} else {
			if (b[0] != testBuf[(int)(expectedPosition % testBuf.length)])
				throw new Exception("Invalid byte read " + b[0] + ", expected is " + testBuf[(int)(expectedPosition % testBuf.length)]);
			
			p = io.getPosition();
			if (p != expectedPosition + 1)
				throw new Exception("getPosition returned an invalid position " + p + ", expected is " + (expectedPosition + 1));
			
			p = io.seekSync(SeekType.FROM_CURRENT, -1);
			if (p != expectedPosition)
				throw new Exception("Invalid seek: returned position " + p + ", expected is " + expectedPosition);
		}
	}
	
	@Test
	public void testSeekAsync() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			IAsync<?> sp = new Async<>(true);
			
			sp = testSeekAsync(sp, io, SeekType.FROM_BEGINNING, 0, 0, size);
			sp = testSeekAsync(sp, io, SeekType.FROM_END, 0, size, size);
			if (size > 234567) {
				sp = testSeekAsync(sp, io, SeekType.FROM_BEGINNING, 123456, 123456, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 12345, 123456 + 12345, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_END, 234567, size - 234567, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 999, size - 234567 + 999, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, -8888, size - 234567 + 999 - 8888, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 0, size - 234567 + 999 - 8888, size);
			} else if (size > 0) {
				sp = testSeekAsync(sp, io, SeekType.FROM_BEGINNING, 1, 1, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 9, 10, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, -4, 6, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_END, -6, size, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_END, 6, size-6, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 2, size-4, size);
				sp = testSeekAsync(sp, io, SeekType.FROM_BEGINNING, 8, 8, size);
			} else {
				sp = testSeekAsync(sp, io, SeekType.FROM_CURRENT, 10, 0, size);
			}
			
			sp.blockThrow(0);
		}
	}
	
	private IAsync<?> testSeekAsync(IAsync<?> startOn, IO.Readable.Seekable io, SeekType type, long move, long expectedPosition, long size) {
		Async<Exception> sp = new Async<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		Consumer<Pair<Long,IOException>> ondone;
		if ((expectedPosition % 3) == 0 && size > 0) {
			onDoneBefore.set(true);
			ondone = null;
		} else {
			ondone = param -> onDoneBefore.set(true);
		}

		startOn.onDone(new Runnable() {
			@Override
			public void run() {
				if (startOn.hasError()) {
					sp.error(startOn.getError());
					return;
				}
				AsyncSupplier<Long, IOException> seek = io.seekAsync(type, move, ondone);
				seek.onDone(new Runnable() {
					@Override
					public void run() {
						if (seek.hasError()) {
							sp.error(seek.getError());
							return;
						}
						if (!onDoneBefore.get()) {
							sp.error(new Exception("Method seekAsync didn't call ondone before listeners"));
							return;
						}
						if (ondone != null)
							onDoneBefore.set(false);
						long p = seek.getResult().longValue();
						if (p != expectedPosition) {
							sp.error(new Exception("Invalid seek: returned position " + p + ", expected is " + expectedPosition + " after seek(" + type + ", " + move + ")"));
							return;
						}
						try { p = io.getPosition(); }
						catch (IOException e) {
							sp.error(e);
							return;
						}
						if (p != expectedPosition) {
							sp.error(new Exception("getPosition returned an invalid position " + p + ", expected is " + expectedPosition));
							return;
						}
						byte[] b = new byte[1];
						AsyncSupplier<Integer, IOException> read = io.readAsync(ByteBuffer.wrap(b));
						read.onDone(new Runnable() {
							@Override
							public void run() {
								if (read.hasError()) {
									sp.error(read.getError());
									return;
								}
								int nb = read.getResult().intValue();
								if (expectedPosition == size) {
									if (nb > 0) {
										sp.error(new Exception("Can read after file"));
										return;
									}
									sp.unblock();
								} else {
									if (b[0] != testBuf[(int)(expectedPosition % testBuf.length)]) {
										sp.error(new Exception("Invalid byte read " + b[0] + ", expected is " + testBuf[(int)(expectedPosition % testBuf.length)]));
										return;
									}
									long p;
									try { p = io.getPosition(); }
									catch (IOException e) {
										sp.error(e);
										return;
									}
									if (p != expectedPosition + 1) {
										sp.error(new Exception("getPosition returned an invalid position " + p + ", expected is " + (expectedPosition + 1)));
										return;
									}
									AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_CURRENT, -1);
									seek.onDone(new Runnable() {
										@Override
										public void run() {
											if (seek.hasError()) {
												sp.error(seek.getError());
												return;
											}
											long p = seek.getResult().longValue();
											if (p != expectedPosition) {
												sp.error(new Exception("Invalid seek: returned position " + p + ", expected is " + expectedPosition));
												return;
											}
											sp.unblock();
										}
									});
								}
							}
						});
					}
				});
			}
		});
		return sp;
	}
	
	@Test
	public void testSeekSyncWrong() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			long s;
			
			s = io.seekSync(SeekType.FROM_BEGINNING, -10);
			if (s != 0) throw new Exception("Invalid position " + s + ", expected is 0");
			s = io.seekSync(SeekType.FROM_END, -10);
			if (s != size) throw new Exception("Invalid position " + s + ", expected is " + size);
			s = io.seekSync(SeekType.FROM_CURRENT, 10);
			if (s != size) throw new Exception("Invalid position " + s + ", expected is " + size);
			s = io.seekSync(SeekType.FROM_BEGINNING, -1);
			if (s != 0) throw new Exception("Invalid position " + s + ", expected is 0");
			s = io.seekSync(SeekType.FROM_CURRENT, -10);
			if (s != 0) throw new Exception("Invalid position " + s + ", expected is 0");
		}		
	}
	
	@Test
	public void testSeekAsyncWrong() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			Async<Exception> sp = new Async<>();
			
			AsyncSupplier<Long, IOException> op = io.seekAsync(SeekType.FROM_BEGINNING, -10);
			op.onDone(new Runnable() {
				@Override
				public void run() {
					if (op.hasError()) { sp.error(op.getError()); return; }
					if (op.getResult().longValue() != 0) {
						sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is 0"));
						return;
					}
					AsyncSupplier<Long, IOException> op = io.seekAsync(SeekType.FROM_END, -10);
					op.onDone(new Runnable() {
						@Override
						public void run() {
							if (op.hasError()) { sp.error(op.getError()); return; }
							if (op.getResult().longValue() != size) {
								sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is " + size));
								return;
							}
							
							AsyncSupplier<Long, IOException> op = io.seekAsync(SeekType.FROM_CURRENT, 10);
							op.onDone(new Runnable() {
								@Override
								public void run() {
									if (op.hasError()) { sp.error(op.getError()); return; }
									if (op.getResult().longValue() != size) {
										sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is " + size));
										return;
									}
									AsyncSupplier<Long, IOException> op = io.seekAsync(SeekType.FROM_BEGINNING, -1);
									op.onDone(new Runnable() {
										@Override
										public void run() {
											if (op.hasError()) { sp.error(op.getError()); return; }
											if (op.getResult().longValue() != 0) {
												sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is 0"));
												return;
											}
											AsyncSupplier<Long, IOException> op = io.seekAsync(SeekType.FROM_CURRENT, -10);
											op.onDone(new Runnable() {
												@Override
												public void run() {
													if (op.hasError()) { sp.error(op.getError()); return; }
													if (op.getResult().longValue() != 0) {
														sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is 0"));
														return;
													}
													sp.unblock();
												}
											});
										}
									});
								}
							});
						}
					});
				}
			});
	
			sp.blockThrow(0);
		}
	}
	
	@Test
	public void testSkipSync() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			long pos = 0;
			byte[] b = new byte[testBuf.length * 3];
			while (true) {
				int nb = io.readFullySync(ByteBuffer.wrap(b));
				if (nb < 0) nb = 0;
				if (nb < b.length) {
					if (pos + nb < size)
						throw new AssertionError("Only " + nb + " byte(s) read at " + pos);
					checkBuffer(b, 0, nb, pos);
				}
				pos += nb;
				long skipped = io.skipSync(testBuf.length / 4);
				if (skipped != testBuf.length / 4) {
					if (pos + skipped != size)
						throw new AssertionError(skipped + " byte(s) skipped at " + pos);
				}
				pos += skipped;
				nb = io.readFullySync(ByteBuffer.wrap(b, 0, testBuf.length));
				if (nb < 0) nb = 0;
				if (nb < testBuf.length) {
					if (pos + nb < size)
						throw new AssertionError("Only " + nb + " byte(s) read at " + pos);
					checkBuffer(b, 0, nb, pos);
				}
				pos += nb;
				skipped = io.skipSync(-testBuf.length / 3);
				if (skipped != -testBuf.length / 3) {
					if (pos + skipped != 0)
						throw new AssertionError(skipped + " byte(s) skipped at " + pos);
				}
				boolean isEnd = pos == size;
				pos += skipped;
				if (isEnd) break;
			}
			// we should be able to skip backward
			io.seekSync(SeekType.FROM_END, 0);
			Assert.assertEquals(-(size/2), io.skipSync(-(size/2)));
			Assert.assertEquals(size - (size/2), io.getPosition());
			Assert.assertEquals(-(size - (size/2)), io.skipSync(-size));
			Assert.assertEquals(0, io.getPosition());
			// skipping to a negative value should go to the start
			Assert.assertEquals(0, io.skipSync(-10));
			Assert.assertEquals(0, io.getPosition());
		}
	}

	@Test
	public void testSkipAsync() throws Exception {
		long size = getFileSize();
		try (IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size)) {
			long pos = 0;
			byte[] b = new byte[testBuf.length * 3];
			while (true) {
				int nb = io.readFullySync(ByteBuffer.wrap(b));
				if (nb < 0) nb = 0;
				if (nb < b.length) {
					if (pos + nb < size)
						throw new AssertionError("Only " + nb + " byte(s) read at " + pos);
					checkBuffer(b, 0, nb, pos);
				}
				pos += nb;
				AsyncSupplier<Long, IOException> skipped = io.skipAsync(testBuf.length / 4);
				skipped.blockThrow(0);
				if (skipped.getResult().longValue() != testBuf.length / 4) {
					if (pos + skipped.getResult().longValue() != size)
						throw new AssertionError(skipped.getResult().longValue() + " byte(s) skipped at " + pos);
				}
				pos += skipped.getResult().longValue();
				nb = io.readFullySync(ByteBuffer.wrap(b, 0, testBuf.length));
				if (nb < 0) nb = 0;
				if (nb < testBuf.length) {
					if (pos + nb < size)
						throw new AssertionError("Only " + nb + " byte(s) read at " + pos);
					checkBuffer(b, 0, nb, pos);
				}
				pos += nb;
				MutableBoolean onDoneCalled = new MutableBoolean(false);
				Consumer<Pair<Long, IOException>> ondone = p -> {
					onDoneCalled.set(true);
				};
				skipped = io.skipAsync(-testBuf.length / 3, ondone);
				skipped.blockThrow(0);
				if (skipped.getResult().longValue() != -testBuf.length / 3) {
					if (pos + skipped.getResult().longValue() != 0)
						throw new AssertionError(skipped.getResult().longValue() + " byte(s) skipped at " + pos);
				}
				Assert.assertTrue(onDoneCalled.get());
				boolean isEnd = pos == size;
				pos += skipped.getResult().longValue();
				if (isEnd) break;
			}
			io.seekSync(SeekType.FROM_BEGINNING, 0);
			Assert.assertEquals(0,  io.getPosition());
			Assert.assertEquals(0, io.skipAsync(-10).blockResult(0).longValue());
			Assert.assertEquals(0,  io.getPosition());
			Assert.assertEquals(testBuf.length * nbBuf, io.skipAsync(testBuf.length * nbBuf + 200).blockResult(0).longValue());
			Assert.assertEquals(testBuf.length * nbBuf,  io.getPosition());
			MutableBoolean onDoneCalled = new MutableBoolean(false);
			Consumer<Pair<Long, IOException>> ondone = p -> {
				onDoneCalled.set(true);
			};
			Assert.assertEquals(0L, io.skipAsync(10, ondone).blockResult(10000).longValue());
			Assert.assertTrue(onDoneCalled.get());
		}
	}
	
	private void checkBuffer(byte[] b, int off, int len, long pos) {
		if (len == 0) return;
		int i = (int)(pos % testBuf.length);
		int l = testBuf.length - i;
		if (l > len) l = len;
		if (!ArrayUtil.equals(b, off, testBuf, i, l))
			throw new AssertionError("Invalid read at " + pos);
		if (l == len) return;
		checkBuffer(b, off + l, len - l, pos + l);
	}
	
}
