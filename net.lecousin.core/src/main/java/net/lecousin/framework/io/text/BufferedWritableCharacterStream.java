package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;

/** Implement a buffered writable character stream from a writable IO. */
public class BufferedWritableCharacterStream implements ICharacterStream.Writable.Buffered {

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
	public byte getPriority() {
		return output.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		output.setPriority(priority);
	}
	
	private SynchronizationPoint<IOException> flushing = null;

	private void encodeAndWrite() {
		new Task.Cpu<Void,NoException>("Encoding characters", output.getPriority()) {
			@Override
			public Void run() {
				encodedBuffer.clear();
				CoderResult result = encoder.encode(cb2, encodedBuffer, false);
				if (result.isError()) {
					flushing.error(new IOException("Encoding error"));
					return null;
				}
				encodedBuffer.flip();
				AsyncWork<Integer,IOException> writing = output.writeAsync(encodedBuffer);
				writing.listenInline(new Runnable() {
					@Override
					public void run() {
						if (!writing.isSuccessful())
							flushing.error(writing.getError());
						else if (result.isOverflow())
							encodeAndWrite();
						else {
							cb2.clear();
							flushing.unblock();
						}
					}
				});
				return null;
			}
		}.start();
	}
	
	private ISynchronizationPoint<IOException> finalFlush(SynchronizationPoint<IOException> sp, boolean flushOnly) {
		encodedBuffer.clear();
		CoderResult result;
		if (!flushOnly) {
			cb2.limit(pos);
			result = encoder.encode(cb2, encodedBuffer, true);
			if (!result.isOverflow()) {
				flushOnly = true;
				result = encoder.flush(encodedBuffer);
			}
		} else
			result = encoder.flush(encodedBuffer);
		encodedBuffer.flip();
		AsyncWork<Integer,IOException> writing;
		if (encodedBuffer.hasRemaining())
			writing = output.writeAsync(encodedBuffer);
		else
			writing = new AsyncWork<>(Integer.valueOf(0), null);
		if (!result.isOverflow()) {
			if (sp == null)
				return writing;
			writing.listenInline(sp);
			return sp;
		}
		if (sp == null)
			sp = new SynchronizationPoint<>();
		SynchronizationPoint<IOException> spp = sp;
		boolean fo = flushOnly;
		writing.listenInline(new Runnable() {
			@Override
			public void run() {
				if (!writing.isSuccessful())
					spp.error(writing.getError());
				else
					finalFlush(spp, fo);
			}
		});
		return sp;
	}
	
	private void flushBuffer() throws IOException {
		do {
			SynchronizationPoint<IOException> sp;
			synchronized (output) {
				if (flushing != null && flushing.isUnblocked()) {
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
					flushing = new SynchronizationPoint<>();
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

	private SynchronizationPoint<IOException> flushBufferAsync() {
		SynchronizationPoint<IOException> sp;
		synchronized (output) {
			if (flushing != null && flushing.isUnblocked()) {
				if (flushing.hasError())
					return new SynchronizationPoint<>(flushing.getError());
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
				flushing = new SynchronizationPoint<>();
				cb2.limit(pos);
				encodeAndWrite();
				pos = 0;
				return new SynchronizationPoint<>(true);
			}
			sp = flushing;
		}
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		sp.listenInline(
			() -> { flushBufferAsync().listenInline(result); },
			result
		);
		return result;
	}
	
	@Override
	public ISynchronizationPoint<IOException> flush() {
		SynchronizationPoint<IOException> sp;
		do {
			synchronized (output) {
				if (flushing == null || flushing.isUnblocked()) {
					if (pos == 0)
						return finalFlush(null, false);
					try { flushBuffer(); }
					catch (IOException e) { return new SynchronizationPoint<IOException>(e); }
				}
				sp = flushing;
				break;
			}
		} while (true);
		SynchronizationPoint<IOException> sp2 = new SynchronizationPoint<>();
		sp.listenInline(new Runnable() {
			@Override
			public void run() {
				if (sp.hasError())
					sp2.error(sp.getError());
				else
					flush().listenInline(sp2);
			}
		});
		return sp2;
	}
	
	@Override
	public void write(char c) throws IOException {
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
	public ISynchronizationPoint<IOException> writeAsync(char[] c, int off, int len) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		writeAsync(c, off, len, result);
		return result;
	}
	
	private void writeAsync(char[] c, int off, int len, SynchronizationPoint<IOException> result) {
		new Task.Cpu<Void, NoException>("BufferedWritableCharacterStream.writeAsync", output.getPriority()) {
			@Override
			public Void run() {
				int l = len > buffer.length - pos ? buffer.length - pos : len;
				System.arraycopy(c, off, buffer, pos, l);
				pos += l;
				if (l == len) {
					if (pos == buffer.length)
						flushBufferAsync().listenInline(result);
					else
						result.unblock();
					return null;
				}
				flushBufferAsync().listenInline(
					() -> {
						writeAsync(c, off + l, len - l, result);
					},
					result
				);
				return null;
			}
		}.start();
	}
	
	@Override
	public void close() throws Exception {
		ISynchronizationPoint<IOException> flush = flush();
		flush.block(0);
		if (flush.hasError()) {
			try { output.close(); } catch (Throwable t) { /* ignore */ }
			throw flush.getError();
		}
		output.close();
	}
	
	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		ISynchronizationPoint<IOException> flush = flush();
		flush.listenInline(new Runnable() {
			@Override
			public void run() {
				ISynchronizationPoint<IOException> close = output.closeAsync();
				close.listenInline(new Runnable() {
					@Override
					public void run() {
						if (flush.hasError()) sp.error(flush.getError());
						else if (close.hasError()) sp.error(close.getError());
						else sp.unblock();
					}
				});
			}
		});
		return sp;
	}
}
