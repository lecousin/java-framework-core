package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

public abstract class TestReadableSeekable extends TestIO.UsingGeneratedTestFiles {

	protected TestReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createReadableSeekableFromFile(openFile(), getFileSize());
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testSeekableByteByByteSync() throws Exception {
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize());
		byte[] b = new byte[1];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		while (!offsets.isEmpty()) {
			if (nbBuf > 1000 && (offsets.size() % 100) == 99) {
				// make the test faster
				for (int skip = 0; skip < 70 && !offsets.isEmpty(); ++skip)
					offsets.remove(rand.nextInt(offsets.size()));
				continue;
			}
			int i = rand.nextInt(offsets.size());
			Integer offset = offsets.remove(i);
			for (int j = 0; j < testBuf.length; ++j) {
				buffer.clear();
				if (io.readSync(offset.intValue()*testBuf.length+j, buffer) != 1)
					throw new Exception("Unexpected end of stream at " + (i*testBuf.length+j));
				if (b[0] != testBuf[j])
					throw new Exception("Invalid byte "+(b[0]&0xFF)+" at "+(i*testBuf.length+j));
			}
		}
		buffer.clear();
		if (io.readSync((long)nbBuf * testBuf.length, buffer) > 0)
			throw new Exception("Byte read after the end of the file");
		io.close();
	}

	@SuppressWarnings({ "resource" })
	@Test
	public void testSeekableByteByByteAsync() throws Exception {
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize());
		byte[] b = new byte[1];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
		for (int i = 0; i < nbBuf; ++i) offsets.add(Integer.valueOf(i));
		
		MutableInteger j = new MutableInteger(0);
		MutableInteger offset = new MutableInteger(nbBuf > 0 ? offsets.remove(rand.nextInt(offsets.size())).intValue() : 0);
		if (nbBuf == 0) j.set(testBuf.length);
		
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		RunnableWithParameter<Pair<Integer,IOException>> ondone = new RunnableWithParameter<Pair<Integer,IOException>>() {
			@Override
			public void run(Pair<Integer, IOException> param) {
				onDoneBefore.set(true);
			}
		};
		
		Mutable<AsyncWork<Integer,IOException>> read = new Mutable<>(null);
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				if (read.get().hasError()) {
					sp.error(read.get().getError());
					return;
				}
				if (!onDoneBefore.get()) {
					sp.error(new Exception("Method readAsync didn't call ondone before listeners"));
					return;
				}
				onDoneBefore.set(false);
				
				if (offsets.isEmpty() && j.get() == testBuf.length) {
					if (read.get().getResult().intValue() > 0) {
						sp.error(new Exception("Byte read after the end of the file"));
						return;
					}
					sp.unblock();
					return;
				}
				if (read.get().getResult().intValue() != 1) {
					sp.error(new Exception("Unexpected end of stream at " + (offset.get()*testBuf.length+j.get())));
					return;
				}
				if (b[0] != testBuf[j.get()]) {
					sp.error(new Exception("Invalid byte "+(b[0]&0xFF)+" at "+(offset.get()*testBuf.length+j.get())));
					return;
				}

				if (j.inc() == testBuf.length) {
					if (offsets.isEmpty()) {
						// read again to test we cannot read beyond the end of the file
						offset.set(nbBuf-1);
					} else {
						if (nbBuf > 1000 && (offsets.size() % 100) == 99) {
							// make the test faster
							for (int skip = 0; skip < 70 && offsets.size() > 1; ++skip)
								offsets.remove(rand.nextInt(offsets.size()));
						}

						offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
						j.set(0);
					}
				}

				buffer.clear();
				read.set(io.readAsync(offset.get()*testBuf.length+j.get(), buffer, ondone));
				read.get().listenInline(this);
			}
		};
		
		read.set(io.readAsync(offset.get()*testBuf.length+j.get(), buffer, ondone));
		read.get().listenInline(listener);

		sp.block(0);
		if (sp.hasError()) throw sp.getError();
		
		io.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testSeekableBufferByBufferFullySync() throws Exception {
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize());
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
		}
		buffer.clear();
		if (io.readFullySync((long)nbBuf * testBuf.length, buffer) > 0)
			throw new Exception("Bytes read after the end of the file");
		io.close();
	}
	
	@Test
	public void testSeekableBufferByBufferFullyAsync() throws Exception {
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize());
		_testSeekableBufferByBufferFullyAsync(io);
		io.close();
	}
	private void _testSeekableBufferByBufferFullyAsync(IO.Readable.Seekable io) throws Exception {
		byte[] b = new byte[testBuf.length];
		ByteBuffer buffer = ByteBuffer.wrap(b);
		ArrayList<Integer> offsets = new ArrayList<Integer>(nbBuf);
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
		
		Mutable<AsyncWork<Integer,IOException>> read = new Mutable<>(null);
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				if (read.get().hasError()) {
					sp.error(read.get().getError());
					return;
				}
				if (!onDoneBefore.get()) {
					sp.error(new Exception("Method readFullyAsync didn't call ondone before listeners"));
					return;
				}
				onDoneBefore.set(false);
				
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

				if (offsets.isEmpty()) {
					// read again to test we cannot read beyond the end of the file
					offset.set(nbBuf);
				} else {
					offset.set(offsets.remove(rand.nextInt(offsets.size())).intValue());
				}

				buffer.clear();
				read.set(io.readFullyAsync(offset.get()*testBuf.length, buffer, ondone));
				read.get().listenInline(this);
			}
		};
		
		read.set(io.readFullyAsync(offset.get()*testBuf.length, buffer, ondone));
		read.get().listenInline(listener);

		sp.block(0);
		if (sp.hasError()) throw sp.getError();
	}
	

	@Test
	public void testConcurrentAccessToSeekableBufferByBufferFullyAsync() throws Exception {
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), getFileSize());
		int nbConc = Runtime.getRuntime().availableProcessors() * 5;
		ArrayList<Task<Void,Exception>> tasks = new ArrayList<>(nbConc);
		for (int t = 0; t < nbConc; ++t) {
			Task<Void,Exception> task = new Task.Cpu<Void,Exception>("Test Concurrent access to IO.Readable.Seekable",Task.PRIORITY_NORMAL) {
				@Override
				public Void run() throws Exception {
					_testSeekableBufferByBufferFullyAsync(io);
					return null;
				}
			};
			task.start();
			tasks.add(task);
		}
		Threading.waitFinished(tasks);
		io.close();
		for (Task<Void,Exception> task : tasks)
			if (!task.isSuccessful())
				throw task.getError();
	}
	
	@Test
	public void testSeekSync() throws Exception {
		long size = getFileSize();
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size);
		
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
		
		io.close();
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
	
	@SuppressWarnings("resource")
	@Test
	public void testSeekAsync() throws Exception {
		long size = getFileSize();
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size);
		
		ISynchronizationPoint<?> sp = new SynchronizationPoint<>(true);
		
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
		
		sp.block(0);
		if (sp.hasError()) throw sp.getError();
		io.close();
	}
	
	private ISynchronizationPoint<?> testSeekAsync(ISynchronizationPoint<?> startOn, IO.Readable.Seekable io, SeekType type, long move, long expectedPosition, long size) {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		RunnableWithParameter<Pair<Long,IOException>> ondone = new RunnableWithParameter<Pair<Long,IOException>>() {
			@Override
			public void run(Pair<Long, IOException> param) {
				onDoneBefore.set(true);
			}
		};

		startOn.listenInline(new Runnable() {
			@Override
			public void run() {
				if (startOn.hasError()) {
					sp.error(startOn.getError());
					return;
				}
				AsyncWork<Long, IOException> seek = io.seekAsync(type, move, ondone);
				seek.listenInline(new Runnable() {
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
						onDoneBefore.set(false);
						long p = seek.getResult().longValue();
						if (p != expectedPosition) {
							sp.error(new Exception("Invalid seek: returned position " + p + ", expected is " + expectedPosition));
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
						AsyncWork<Integer, IOException> read = io.readAsync(ByteBuffer.wrap(b));
						read.listenInline(new Runnable() {
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
									AsyncWork<Long, IOException> seek = io.seekAsync(SeekType.FROM_CURRENT, -1);
									seek.listenInline(new Runnable() {
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
	
	@SuppressWarnings("resource")
	@Test
	public void testSeekSyncWrong() throws Exception {
		long size = getFileSize();
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size);
		
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
		
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testSeekAsyncWrong() throws Exception {
		long size = getFileSize();
		IO.Readable.Seekable io = createReadableSeekableFromFile(openFile(), size);

		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		
		AsyncWork<Long, IOException> op = io.seekAsync(SeekType.FROM_BEGINNING, -10);
		op.listenInline(new Runnable() {
			@Override
			public void run() {
				if (op.hasError()) { sp.error(op.getError()); return; }
				if (op.getResult().longValue() != 0) {
					sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is 0"));
					return;
				}
				AsyncWork<Long, IOException> op = io.seekAsync(SeekType.FROM_END, -10);
				op.listenInline(new Runnable() {
					@Override
					public void run() {
						if (op.hasError()) { sp.error(op.getError()); return; }
						if (op.getResult().longValue() != size) {
							sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is " + size));
							return;
						}
						
						AsyncWork<Long, IOException> op = io.seekAsync(SeekType.FROM_CURRENT, 10);
						op.listenInline(new Runnable() {
							@Override
							public void run() {
								if (op.hasError()) { sp.error(op.getError()); return; }
								if (op.getResult().longValue() != size) {
									sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is " + size));
									return;
								}
								AsyncWork<Long, IOException> op = io.seekAsync(SeekType.FROM_BEGINNING, -1);
								op.listenInline(new Runnable() {
									@Override
									public void run() {
										if (op.hasError()) { sp.error(op.getError()); return; }
										if (op.getResult().longValue() != 0) {
											sp.error(new Exception("Invalid position " + op.getResult().longValue() + ", expected is 0"));
											return;
										}
										AsyncWork<Long, IOException> op = io.seekAsync(SeekType.FROM_CURRENT, -10);
										op.listenInline(new Runnable() {
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

		sp.block(0);
		if (sp.hasError()) throw sp.getError();
		io.close();
	}
	
	// TODO test read not fully sync+async
	
	// TODO test concurrent access with bigger buffer size
	// TODO test readfully with bigger buffer size
	
}
