package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Implement a buffered writable character stream from a writable IO. */
public class BufferedWritableCharacterStream extends ConcurrentCloseable<IOException> implements ICharacterStream.Writable.Buffered {

	/** Constructor. */
	public BufferedWritableCharacterStream(IO.Writable output, Charset charset, int bufferSize) {
		this(output, charset.newEncoder(), bufferSize);
	}

	/** Constructor. */
	public BufferedWritableCharacterStream(IO.Writable output, CharsetEncoder encoder, int bufferSize) {
		this.output = output;
		this.encoder = encoder;
		this.buffer = new char[bufferSize];
		this.buffer2 = new char[bufferSize];
		cb = CharBuffer.wrap(buffer);
		cb2 = CharBuffer.wrap(buffer2);
		encodedBuffer = ByteBuffer.allocate(bufferSize * 2);
	}
	
	private IO.Writable output;
	private CharsetEncoder encoder;
	private char[] buffer;
	private char[] buffer2;
	private CharBuffer cb;
	private CharBuffer cb2;
	private ByteBuffer encodedBuffer;
	private int pos = 0;
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return output.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		output = null;
		encoder = null;
		buffer = null;
		buffer2 = null;
		cb = null;
		cb2 = null;
		encodedBuffer = null;
		ondone.unblock();
	}
	
	@Override
	public Priority getPriority() {
		return output.getPriority();
	}
	
	@Override
	public void setPriority(Priority priority) {
		output.setPriority(priority);
	}
	
	@Override
	public String getDescription() {
		return output.getSourceDescription();
	}
	
	@Override
	public Charset getEncoding() {
		return encoder.charset();
	}
	
	private Async<IOException> flushing = null;

	private void encodeAndWrite() {
		Task.cpu("Encoding characters", output.getPriority(), task -> {
			encodedBuffer.clear();
			CoderResult result = encoder.encode(cb2, encodedBuffer, false);
			if (result.isError()) {
				flushing.error(new IOException("Encoding error"));
				return null;
			}
			encodedBuffer.flip();
			AsyncSupplier<Integer,IOException> writing = output.writeAsync(encodedBuffer);
			writing.onDone(() -> {
				if (!writing.isSuccessful())
					flushing.error(writing.getError());
				else if (result.isOverflow())
					encodeAndWrite();
				else {
					cb2.clear();
					flushing.unblock();
				}
			});
			return null;
		}).start();
		operation(flushing);
	}
	
	private IAsync<IOException> finalFlush(Async<IOException> sp, boolean flushOnly) {
		encodedBuffer.clear();
		CoderResult result;
		if (!flushOnly) {
			cb2.limit(pos);
			try {
				result = encoder.encode(cb2, encodedBuffer, true);
			} catch (Exception e) {
				return new Async<>(new IOException("Error finalizing encoding", e));
			}
			if (!result.isOverflow()) {
				flushOnly = true;
				result = encoder.flush(encodedBuffer);
			}
		} else {
			result = encoder.flush(encodedBuffer);
		}
		encodedBuffer.flip();
		AsyncSupplier<Integer,IOException> writing;
		if (encodedBuffer.hasRemaining())
			writing = output.writeAsync(encodedBuffer);
		else
			writing = new AsyncSupplier<>(Integer.valueOf(0), null);
		if (!result.isOverflow()) {
			if (sp == null)
				return writing;
			writing.onDone(sp);
			return operation(sp);
		}
		if (sp == null)
			sp = new Async<>();
		Async<IOException> spp = sp;
		boolean fo = flushOnly;
		writing.onDone(() -> {
			if (!writing.isSuccessful())
				spp.error(writing.getError());
			else
				finalFlush(spp, fo);
		});
		return operation(sp);
	}
	
	private void flushBuffer() throws IOException {
		do {
			Async<IOException> sp;
			synchronized (output) {
				if (flushing != null && flushing.isDone()) {
					if (flushing.hasError())
						throw flushing.getError();
					flushing = null;
				}
				if (flushing == null) {
					char[] tmp1 = buffer2;
					buffer2 = buffer;
					buffer = tmp1;
					CharBuffer tmp2 = cb2;
					cb2 = cb;
					cb = tmp2;
					cb.clear();
					flushing = new Async<>();
					cb2.limit(pos);
					encodeAndWrite();
					pos = 0;
					return;
				}
				sp = flushing;
			}
			sp.block(0);
		} while (true);
	}

	private Async<IOException> flushBufferAsync() {
		Async<IOException> sp;
		synchronized (output) {
			if (flushing != null && flushing.isDone()) {
				if (flushing.hasError())
					return new Async<>(flushing.getError());
				flushing = null;
			}
			if (flushing == null) {
				char[] tmp1 = buffer2;
				buffer2 = buffer;
				buffer = tmp1;
				CharBuffer tmp2 = cb2;
				cb2 = cb;
				cb = tmp2;
				cb.clear();
				flushing = new Async<>();
				cb2.limit(pos);
				encodeAndWrite();
				pos = 0;
				return new Async<>(true);
			}
			sp = flushing;
		}
		Async<IOException> result = new Async<>();
		sp.onDone(() -> flushBufferAsync().onDone(result), result);
		return result;
	}
	
	@Override
	public IAsync<IOException> flush() {
		Async<IOException> sp;
		do {
			synchronized (output) {
				if (flushing == null || flushing.isDone()) {
					if (pos == 0)
						return finalFlush(null, false);
					try { flushBuffer(); }
					catch (IOException e) { return new Async<>(e); }
				}
				sp = flushing;
				break;
			}
		} while (true);
		Async<IOException> sp2 = new Async<>();
		sp.onDone(() -> {
			if (sp.hasError())
				sp2.error(sp.getError());
			else
				flush().onDone(sp2);
		});
		return sp2;
	}
	
	@Override
	public void writeSync(char c) throws IOException {
		buffer[pos++] = c;
		if (pos == buffer.length)
			flushBuffer();
	}
	
	@Override
	public void writeSync(char[] c, int off, int len) throws IOException {
		while (len > 0) {
			int l = len > buffer.length - pos ? buffer.length - pos : len;
			System.arraycopy(c, off, buffer, pos, l);
			pos += l;
			if (pos == buffer.length)
				flushBuffer();
			off += l;
			len -= l;
		}
	}

	@Override
	public IAsync<IOException> writeAsync(char c) {
		buffer[pos++] = c;
		if (pos == buffer.length)
			return flushBufferAsync();
		return new Async<>(true);
	}
	
	@Override
	public IAsync<IOException> writeAsync(char[] c, int off, int len) {
		Async<IOException> result = new Async<>();
		writeAsync(c, off, len, result);
		return operation(result);
	}
	
	private void writeAsync(char[] c, int off, int len, Async<IOException> result) {
		int l = len > buffer.length - pos ? buffer.length - pos : len;
		System.arraycopy(c, off, buffer, pos, l);
		pos += l;
		if (l == len) {
			if (pos == buffer.length)
				flushBufferAsync().onDone(result);
			else
				result.unblock();
			return;
		}
		flushBufferAsync().thenStart("BufferedWritableCharacterStream.writeAsync", output.getPriority(),
			() -> writeAsync(c, off + l, len - l, result), result);
	}
}
