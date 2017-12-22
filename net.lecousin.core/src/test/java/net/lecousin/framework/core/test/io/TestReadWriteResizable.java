package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;

public abstract class TestReadWriteResizable extends TestIO {

	protected abstract 
	<T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize & IO.Resizable> 
	T openReadWriteResizable() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return openReadWriteResizable();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
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
		if (s != 12) throw new Exception("Write error: new position is " + s + ", expected is 12");

		io.setSizeSync(10);
		s = io.getSizeSync();
		if (s != 10) throw new Exception("Resize error: new size is " + s + ", expected is 10");
		s = io.getPosition();
		if (s != 10) throw new Exception("Resize error: new position is " + s + ", expected is 10");

		io.setSizeSync(0);
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
		
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize & IO.Resizable>
	void testResizeAsync() throws Exception {
		T io = openReadWriteResizable();
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		AsyncWork<Long,IOException> op = io.getSizeAsync();
		op.listenInline(new Runnable() {
			@Override
			public void run() {
				if (op.hasError()) { sp.error(op.getError()); return; }
				if (op.getResult().longValue() != 0) {
					sp.error(new Exception("ReadWrite must be empty at first"));
					return;
				}
				AsyncWork<Void,IOException> op = io.setSizeAsync(1);
				op.listenInline(new Runnable() {
					@Override
					public void run() {
						if (op.hasError()) { sp.error(op.getError()); return; }
						AsyncWork<Long,IOException> op = io.getSizeAsync();
						op.listenInline(new Runnable() {
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
								AsyncWork<Integer,IOException> op = io.writeAsync(ByteBuffer.wrap(new byte[10]));
								op.listenInline(new Runnable() {
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
										AsyncWork<Long,IOException> op = io.getSizeAsync();
										op.listenInline(new Runnable() {
											@Override
											public void run() {
												if (op.hasError()) { sp.error(op.getError()); return; }
												if (op.getResult().longValue() != 10) {
													sp.error(new Exception("Resize error: new size is " + op.getResult().longValue() + ", expected is 10"));
													return;
												}
												AsyncWork<Void,IOException> op = io.setSizeAsync(3);
												op.listenInline(new Runnable() {
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
														AsyncWork<Long,IOException> op = io.getSizeAsync();
														op.listenInline(new Runnable() {
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
