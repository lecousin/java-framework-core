package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Simple implementation of a buffered writable using 2 buffers.<br/>
 * The first buffer is first filled with written data.
 * Once it is full, it is flushed to the underlying writable, and new data are written on the second buffer.<br/>
 * If the second buffer is full before the first one is flushed, operations are blocking.
 */
public class SimpleBufferedWritable extends ConcurrentCloseable implements IO.Writable.Buffered {

	/** Constructor. */
	public SimpleBufferedWritable(Writable out, int bufferSize) {
		this.out = out;
		buffer = new byte[bufferSize];
		buffer2 = new byte[bufferSize];
		bb = ByteBuffer.wrap(buffer);
		bb2 = ByteBuffer.wrap(buffer2);
	}
	
	private Writable out;
	private byte[] buffer;
	private byte[] buffer2;
	private ByteBuffer bb;
	private ByteBuffer bb2;
	private int pos = 0;
	private AsyncWork<Integer,IOException> writing = null;
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		return new SynchronizationPoint<>(true);
	}
	
	private void flushBuffer() throws IOException {
		do {
			AsyncWork<Integer,IOException> sp;
			synchronized (out) {
				if (writing != null && writing.isUnblocked()) {
					if (writing.hasError())
						throw writing.getError();
					writing = null;
				}
				if (writing == null) {
					byte[] tmp1 = buffer2;
					buffer2 = buffer;
					buffer = tmp1;
					ByteBuffer tmp2 = bb2;
					bb2 = bb;
					bb = tmp2;
					bb.clear();
					writing = out.writeAsync(bb2);
					pos = 0;
					return;
				}
				sp = writing;
			}
			sp.block(0);
		} while (true);
	}
	
	private AsyncWork<Integer,IOException> flushBufferAsync() throws IOException {
		synchronized (out) {
			if (writing != null && writing.isUnblocked()) {
				if (writing.hasError())
					throw writing.getError();
				writing = null;
			}
			if (writing == null) {
				byte[] tmp1 = buffer2;
				buffer2 = buffer;
				buffer = tmp1;
				ByteBuffer tmp2 = bb2;
				bb2 = bb;
				bb = tmp2;
				bb.clear();
				writing = out.writeAsync(bb2);
				pos = 0;
				return null;
			}
			return writing;
		}
	}
	
	@Override
	public ISynchronizationPoint<IOException> flush() {
		if (pos == 0) {
			if (writing == null)
				return new SynchronizationPoint<>(true);
			return writing;
		}
		AsyncWork<Integer,IOException> w;
		synchronized (out) {
			if (writing != null && writing.isUnblocked()) {
				if (writing.hasError())
					return writing;
				writing = null;
			}
			if (writing == null) {
				byte[] tmp1 = buffer2;
				buffer2 = buffer;
				buffer = tmp1;
				ByteBuffer tmp2 = bb2;
				bb2 = bb;
				bb = tmp2;
				bb.clear();
				bb2.limit(pos);
				writing = out.writeAsync(bb2);
				pos = 0;
				return writing;
			}
			w = writing;
		}
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		w.listenInline(() -> {
			flush().listenInline(sp);
		}, sp);
		return operation(sp);
	}
	
	@Override
	public void write(byte b) throws IOException {
		buffer[pos++] = b;
		if (pos == buffer.length)
			flushBuffer();
	}
	
	@Override
	public void write(byte[] buf, int offset, int length) throws IOException {
		do {
			int len = length > buffer.length - pos ? buffer.length - pos : length;
			System.arraycopy(buf, offset, buffer, pos, len);
			pos += len;
			if (pos == buffer.length)
				flushBuffer();
			if (len == length) return;
			length -= len;
			offset += len;
		} while (true);
	}
	
	@Override
	public int writeSync(ByteBuffer buf) throws IOException {
		int done = 0;
		do {
			int len = buf.remaining();
			if (len > buffer.length - pos) len = buffer.length - pos;
			buf.get(buffer, pos, len);
			pos += len;
			done += len;
			if (pos == buffer.length)
				flushBuffer();
			if (buf.remaining() == 0) return done;
		} while (true);
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		AsyncWork<Integer,IOException> result = new AsyncWork<Integer, IOException>();
		writeAsync(buf, 0, result, ondone);
		return result;
	}
	
	private void writeAsync(
		ByteBuffer buf, int done, AsyncWork<Integer,IOException> result, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		Task<Void,NoException> task = new Task.Cpu<Void,NoException>("Write async to SimpleBufferedWritable", out.getPriority()) {
			@Override
			public Void run() {
				int d = done;
				do {
					int len = buf.remaining();
					if (len > buffer.length - pos) len = buffer.length - pos;
					buf.get(buffer, pos, len);
					pos += len;
					d += len;
					if (pos == buffer.length) {
						AsyncWork<Integer,IOException> flush;
						try { flush = flushBufferAsync(); }
						catch (IOException e) {
							if (ondone != null) ondone.run(new Pair<>(null, e));
							result.unblockError(e);
							return null;
						}
						if (flush != null) {
							int dd = d;
							flush.listenInline(new Runnable() {
								@Override
								public void run() {
									if (!flush.isSuccessful()) {
										if (ondone != null) ondone.run(new Pair<>(null, flush.getError()));
										result.unblockError(flush.getError());
										return;
									}
									writeAsync(buf, dd, result, ondone);
								}
							});
							return null;
						}
					}
					if (buf.remaining() == 0) {
						if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(d), null));
						result.unblockSuccess(Integer.valueOf(d));
						return null;
					}
				} while (true);
			}
		};
		operation(task.start());
	}

	@Override
	public String getSourceDescription() { return out.getSourceDescription(); }
	
	@Override
	public IO getWrappedIO() { return out; }
	
	@Override
	public byte getPriority() { return out.getPriority(); }
	
	@Override
	public void setPriority(byte priority) { out.setPriority(priority); }
	
	@Override
	public TaskManager getTaskManager() { return Threading.getCPUTaskManager(); }

	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		ISynchronizationPoint<IOException> flush = flush();
		flush.listenInline(new Runnable() {
			@Override
			public void run() {
				ISynchronizationPoint<Exception> close = out.closeAsync();
				if (flush.hasError())
					sp.error(flush.getError());
				else
					close.listenInline(sp);
			}
		});
		return sp;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		out = null;
		buffer = null;
		buffer2 = null;
		bb = null;
		bb2 = null;
		ondone.unblock();
	}
	
}
