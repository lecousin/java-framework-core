package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.util.production.simple.Producer;
import net.lecousin.framework.concurrent.util.production.simple.Production;
import net.lecousin.framework.io.IO;

/**
 * Take a readable IO to produce ByteBuffer.
 */
public class IOReaderAsProducer implements Producer<ByteBuffer> {

	/** Constructor. */
	public IOReaderAsProducer(IO.Readable io, int bufferSize) {
		this.io = io;
		this.bufferSize = bufferSize;
	}
	
	private IO.Readable io;
	private int bufferSize;
	private AsyncWork<Integer,IOException> read = null;
	
	@Override
	public AsyncWork<ByteBuffer, IOException> produce(Production<ByteBuffer> production) {
		AsyncWork<ByteBuffer, IOException> sp = new AsyncWork<>();
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		read = io.readFullyAsync(buffer); // TODO not fully, to be able to produce event not everything can be read
		read.listenInline(new AsyncWorkListener<Integer, IOException>() {
			@Override
			public void ready(Integer result) {
				read = null;
				if (result.intValue() <= 0) {
					sp.unblockSuccess(null);
					return;
				}
				if (buffer.hasRemaining())
					production.endOfProduction();
				buffer.flip();
				sp.unblockSuccess(buffer);
			}
			
			@Override
			public void cancelled(CancelException event) {
				read = null;
				sp.unblockCancel(event);
			}
			
			@Override
			public void error(IOException error) {
				read = null;
				sp.unblockError(error);
			}
		});
		return sp;
	}

	@Override
	public void cancel(CancelException event) {
		AsyncWork<Integer,IOException> r = read;
		if (r != null) r.unblockCancel(event);
	}
}
