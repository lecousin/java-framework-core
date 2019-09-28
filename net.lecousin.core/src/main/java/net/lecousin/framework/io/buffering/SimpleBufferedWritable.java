package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Simple implementation of a buffered writable using 2 buffers.<br/>
 * The first buffer is first filled with written data.
 * Once it is full, it is flushed to the underlying writable, and new data are written on the second buffer.<br/>
 * If the second buffer is full before the first one is flushed, operations are blocking.
 */
public class SimpleBufferedWritable extends ConcurrentCloseable<IOException> implements IO.Writable.Buffered {

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
	private AsyncSupplier<Integer,IOException> writing = null;
	
	@Override
	public IAsync<IOException> canStartWriting() {
		return new Async<>(true);
	}
	
	private void flushBuffer() throws IOException {
		do {
			AsyncSupplier<Integer,IOException> sp;
			synchronized (out) {
				if (writing != null && writing.isDone()) {
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
	
	private AsyncSupplier<Integer,IOException> flushBufferAsync() throws IOException {
		synchronized (out) {
			if (writing != null && writing.isDone()) {
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
	public IAsync<IOException> flush() {
		if (pos == 0) {
			if (writing == null)
				return new Async<>(true);
			return writing;
		}
		AsyncSupplier<Integer,IOException> w;
		synchronized (out) {
			if (writing != null && writing.isDone()) {
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
		Async<IOException> sp = new Async<>();
		w.onDone(() -> flush().onDone(sp), sp);
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
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buf, Consumer<Pair<Integer,IOException>> ondone) {
		AsyncSupplier<Integer,IOException> result = new AsyncSupplier<>();
		writeAsync(buf, 0, result, ondone);
		return result;
	}
	
	private void writeAsync(
		ByteBuffer buf, int done, AsyncSupplier<Integer,IOException> result, Consumer<Pair<Integer,IOException>> ondone
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
						AsyncSupplier<Integer,IOException> flush;
						try { flush = flushBufferAsync(); }
						catch (IOException e) {
							IOUtil.error(e, result, ondone);
							return null;
						}
						if (flush != null) {
							int dd = d;
							flush.onDone(() -> {
								if (!flush.isSuccessful()) {
									IOUtil.error(flush.getError(), result , ondone);
									return;
								}
								writeAsync(buf, dd, result, ondone);
							});
							return null;
						}
					}
					if (buf.remaining() == 0) {
						if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(d), null));
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
	protected IAsync<IOException> closeUnderlyingResources() {
		Async<IOException> sp = new Async<>();
		IAsync<IOException> flush = flush();
		flush.onDone(() -> {
			IAsync<IOException> close = out.closeAsync();
			if (flush.hasError())
				sp.error(flush.getError());
			else
				close.onDone(sp);
		});
		return sp;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		out = null;
		buffer = null;
		buffer2 = null;
		bb = null;
		bb2 = null;
		ondone.unblock();
	}
	
}
