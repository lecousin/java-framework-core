package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;

import org.junit.Test;

public abstract class TestReadWriteResizable extends TestIO {

	protected abstract 
	<T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize & IO.Resizable> 
	T openReadWriteResizable() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return openReadWriteResizable();
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize & IO.Resizable>
	void testResizeSync() throws Exception {
		byte[] buf = new byte[100];
		for (int i = 0; i < 100; ++i) buf[i] = (byte)i;
		T io = openReadWriteResizable();
		long s;
		
		s = io.getSizeSync();
		if (s != 0) throw new Exception("ReadWrite must be empty at first");
		
		io.setSizeSync(1);
		s = io.getSizeSync();
		if (s != 1) throw new Exception("Resize error: new size is " + s + ", expected is 1");
		
		s = io.getPosition();
		if (s != 0) throw new Exception("Resize error: new position is " + s + ", expected is 0");
		
		io.writeSync(ByteBuffer.wrap(buf, 0, 2));
		s = io.getSizeSync();
		if (s != 2) throw new Exception("Write error: new size is " + s + ", expected is 2");
		s = io.getPosition();
		if (s != 2) throw new Exception("Write error: new position is " + s + ", expected is 2");

		io.setSizeSync(1);
		s = io.getSizeSync();
		if (s != 1) throw new Exception("Resize error: new size is " + s + ", expected is 1");
		s = io.getPosition();
		if (s != 1) throw new Exception("Resize error: new position is " + s + ", expected is 1");
		
		io.setSizeSync(10);
		s = io.getSizeSync();
		if (s != 10) throw new Exception("Resize error: new size is " + s + ", expected is 10");
		s = io.getPosition();
		if (s != 1) throw new Exception("Resize error: new position is " + s + ", expected is 1");
		
		io.writeSync(2, ByteBuffer.wrap(buf, 2, 10));
		s = io.getSizeSync();
		if (s != 12) throw new Exception("Write error: new size is " + s + ", expected is 12");
		s = io.getPosition();
		if (s != 1) throw new Exception("Write error: new position is " + s + ", expected is 1");

		io.seekSync(SeekType.FROM_BEGINNING, 12);
		io.setSizeSync(10);
		s = io.getSizeSync();
		if (s != 10) throw new Exception("Resize error: new size is " + s + ", expected is 10");
		s = io.getPosition();
		if (s != 10) throw new Exception("Resize error: new position is " + s + ", expected is 10");

		io.setSizeAsync(0).blockThrow(0);
		s = io.getSizeSync();
		if (s != 0) throw new Exception("Resize error: new size is " + s + ", expected is 0");
		s = io.getPosition();
		if (s != 0) throw new Exception("Resize error: new position is " + s + ", expected is 0");
		
		int nb = io.readSync(ByteBuffer.wrap(buf));
		if (nb > 0) throw new Exception("Can read " + nb + " byte(s) after resize to 0");

		io.setSizeSync(10);
		s = io.getSizeSync();
		if (s != 10) throw new Exception("Resize error: new size is " + s + ", expected is 10");
		s = io.getPosition();
		if (s != 0) throw new Exception("Resize error: new position is " + s + ", expected is 0");
		
		io.writeSync(ByteBuffer.wrap(buf));
		s = io.getSizeSync();
		if (s != 100) throw new Exception("Write error: new size is " + s + ", expected is 100");
		s = io.getPosition();
		if (s != 100) throw new Exception("Write error: new position is " + s + ", expected is 100");
		
		if (io instanceof IO.WritableByteStream) {
			((IO.WritableByteStream)io).write((byte)'a');
			s = io.getSizeSync();
			if (s != 101) throw new Exception("Write error: new size is " + s + ", expected is 101");
			s = io.getPosition();
			if (s != 101) throw new Exception("Write error: new position is " + s + ", expected is 101");
		}
		
		io.setSizeSync(20);
		s = io.getSizeSync();
		if (s != 20) throw new Exception("Write error: new size is " + s + ", expected is 20");
		s = io.getPosition();
		if (s != 20) throw new Exception("Write error: new position is " + s + ", expected is 20");
		
		io.setSizeSync(1024 * 1024);
		s = io.getSizeSync();
		if (s != 1024 * 1024) throw new Exception("Write error: new size is " + s + ", expected is 1024 * 1024");
		io.writeSync(768 * 1024, ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
		s = io.getSizeSync();
		if (s != 1024 * 1024) throw new Exception("Write error: new size is " + s + ", expected is 1024 * 1024");
		io.writeSync(1024 * 1024, ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }));
		s = io.getSizeSync();
		if (s != 1024 * 1024 + 5) throw new Exception("Write error: new size is " + s + ", expected is 1024 * 1024 + 5");
		io.writeSync(2 * 1024 * 1024, ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }));
		s = io.getSizeSync();
		if (s != 2 * 1024 * 1024 + 5) throw new Exception("Write error: new size is " + s + ", expected is 2 * 1024 * 1024 + 5");
		io.setSizeSync(0);
		s = io.getSizeSync();
		if (s != 0) throw new Exception("Write error: new size is " + s + ", expected is 0");
		
		io.writeSync(ByteBuffer.wrap(buf, 0, 2));
		s = io.getSizeSync();
		if (s != 2) throw new Exception("Write error: new size is " + s + ", expected is 2");
		s = io.getPosition();
		if (s != 2) throw new Exception("Write error: new position is " + s + ", expected is 2");
		io.seekSync(SeekType.FROM_BEGINNING, 1);
		s = io.getPosition();
		if (s != 1) throw new Exception("Seek error: new position is " + s + ", expected is 1");
		io.setSizeSync(1);
		s = io.getSizeSync();
		if (s != 1) throw new Exception("Resize error: new size is " + s + ", expected is 1");
		s = io.getPosition();
		if (s != 1) throw new Exception("Resize error: new position is " + s + ", expected is 1");

		io.close();
	}
	
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize & IO.Resizable>
	void testResizeAsync() throws Exception {
		T io = openReadWriteResizable();
		Async<Exception> sp = new Async<>();
		AsyncSupplier<Long,IOException> op = io.getSizeAsync();
		op.onDone(new Runnable() {
			@Override
			public void run() {
				if (op.hasError()) { sp.error(op.getError()); return; }
				if (op.getResult().longValue() != 0) {
					sp.error(new Exception("ReadWrite must be empty at first"));
					return;
				}
				IAsync<IOException> op = io.setSizeAsync(1);
				op.onDone(new Runnable() {
					@Override
					public void run() {
						if (op.hasError()) { sp.error(op.getError()); return; }
						AsyncSupplier<Long,IOException> op = io.getSizeAsync();
						op.onDone(new Runnable() {
							@Override
							public void run() {
								if (op.hasError()) { sp.error(op.getError()); return; }
								if (op.getResult().longValue() != 1) {
									sp.error(new Exception("Resize error: new size is " + op.getResult().longValue() + ", expected is 1"));
									return;
								}
								long p;
								try { p = io.getPosition(); }
								catch (IOException e) { sp.error(e); return; }
								if (p != 0) {
									sp.error(new Exception("Resize error: new position is " + p + ", expected is 0"));
									return;
								}
								AsyncSupplier<Integer,IOException> op = io.writeAsync(ByteBuffer.wrap(new byte[10]));
								op.onDone(new Runnable() {
									@Override
									public void run() {
										if (op.hasError()) { sp.error(op.getError()); return; }
										long p;
										try { p = io.getPosition(); }
										catch (IOException e) { sp.error(e); return; }
										if (p != 10) {
											sp.error(new Exception("Write error: new position is " + p + ", expected is 10"));
											return;
										}
										AsyncSupplier<Long,IOException> op = io.getSizeAsync();
										op.onDone(new Runnable() {
											@Override
											public void run() {
												if (op.hasError()) { sp.error(op.getError()); return; }
												if (op.getResult().longValue() != 10) {
													sp.error(new Exception("Resize error: new size is " + op.getResult().longValue() + ", expected is 10"));
													return;
												}
												IAsync<IOException> op = io.setSizeAsync(3);
												op.onDone(new Runnable() {
													@Override
													public void run() {
														if (op.hasError()) { sp.error(op.getError()); return; }
														long p;
														try { p = io.getPosition(); }
														catch (IOException e) { sp.error(e); return; }
														if (p != 3) {
															sp.error(new Exception("Resize error: new position is " + p + ", expected is 3"));
															return;
														}
														AsyncSupplier<Long,IOException> op = io.getSizeAsync();
														op.onDone(new Runnable() {
															@Override
															public void run() {
																if (op.hasError()) { sp.error(op.getError()); return; }
																if (op.getResult().longValue() != 3) {
																	sp.error(new Exception("Resize error: new size is " + op.getResult().longValue() + ", expected is 3"));
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
					}
				});
			}
		});
		
		sp.blockThrow(0);
		io.close();
	}
	
}
