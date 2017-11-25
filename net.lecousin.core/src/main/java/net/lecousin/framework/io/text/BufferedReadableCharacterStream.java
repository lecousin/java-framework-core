package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;

/** Implement a buffered readable character stream from a readable IO. */
public class BufferedReadableCharacterStream implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public BufferedReadableCharacterStream(IO.Readable input, Charset charset, int bufferSize, int maxBuffers) {
		this(input,
			charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE),
			bufferSize, maxBuffers);
	}
	
	/** Constructor. */
	public BufferedReadableCharacterStream(IO.Readable input, CharsetDecoder decoder, int bufferSize, int maxBuffers) {
		this.input = input;
		this.decoder = decoder;
		if (bufferSize < 64) bufferSize = 64;
		this.bufferSize = bufferSize;
		ready = new TurnArray<>(maxBuffers);
		bytes = ByteBuffer.allocate(bufferSize);
		bytes.limit(0);
		chars = null;
		// start decoding
		if (input instanceof IO.Readable.Buffered)
			((IO.Readable.Buffered)input).canStartReading().listenInline(new Runnable() {
				@Override
				public void run() {
					bufferize();
				}
			});
		else
			bufferize();
	}
	
	private IO.Readable input;
	private CharsetDecoder decoder;
	
	private int bufferSize;
	private TurnArray<CharBuffer> ready; 
	private ByteBuffer bytes;
	private CharBuffer chars;
	private boolean endReached = false;
	private int back = -1;
	private SynchronizationPoint<IOException> nextReady = new SynchronizationPoint<>();
	
	@Override
	public SynchronizationPoint<IOException> canStartReading() {
		synchronized (ready) {
			if (ready.isEmpty())
				return nextReady;
			return new SynchronizationPoint<>(true);
		}
	}
	
	@Override
	public String getSourceDescription() { return input.getSourceDescription(); }
	
	@Override
	public byte getPriority() {
		return input.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		input.setPriority(priority);
	}
	
	public Charset getCharset() {
		return decoder.charset();
	}
	
	private void bufferize() {
		bytes.compact();
		int remaining = bytes.remaining();
		AsyncWork<Integer,IOException> readTask = input.readFullyAsync(bytes);
		Task.Cpu<Void,NoException> decode = new Task.Cpu<Void,NoException>("Decode character stream", input.getPriority()) {
			@Override
			public Void run() {
				if (readTask.isCancelled()) {
					synchronized (ready) {
						nextReady.cancel(readTask.getCancelEvent());
					}
					return null;
				}
				if (readTask.hasError()) {
					synchronized (ready) {
						nextReady.error(readTask.getError());
					}
					return null;
				}
				if (bytes == null)
					return null; // closed
				try {
					int nb = readTask.getResult().intValue();
					bytes.flip();
					boolean end;
					if (nb < remaining) {
						input.closeAsync();
						if (!bytes.hasRemaining()) {
							synchronized (ready) {
								endReached = true;
								nextReady.unblock();
							}
							return null;
						}
						end = true;
					} else
						end = false;
					CharBuffer buf = CharBuffer.allocate(bufferSize);
					CoderResult cr = decoder.decode(bytes, buf, endReached);
					if (cr.isOverflow()) {
						if (buf.position() == 0)
							cr.throwException();
					}
					if (buf.position() == 0) {
						if (end) {
							synchronized (ready) {
								endReached = true;
								nextReady.unblock();
							}
							return null;
						}
						throw new EOFException();
					}
					buf.flip();
					boolean full;
					SynchronizationPoint<IOException> sp;
					synchronized (ready) {
						ready.addLast(buf);
						full = ready.isFull();
						sp = nextReady;
						nextReady = new SynchronizationPoint<>();
						endReached = end;
					}
					sp.unblock();
					if (!full && !endReached)
						bufferize();
					return null;
				} catch (IOException e) {
					synchronized (ready) {
						nextReady.error(e);
					}
					return null;
				} catch (NullPointerException e) {
					// closed
					return null;
				} catch (Throwable t) {
					synchronized (ready) {
						nextReady.error(IO.error(t));
					}
					LCCore.getApplication().getDefaultLogger().error("Error while buffering", t);
					return null;
				}
			}
		};
		decode.startOn(readTask, true);
	}
	
	@Override
	public boolean endReached() {
		return endReached && chars == null;
	}
	
	@Override
	public void back(char c) {
		back = c;
	}
	
	@Override
	public char read() throws EOFException, IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		while (chars == null) {
			boolean full;
			SynchronizationPoint<IOException> sp;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
				if (chars == null && endReached) throw new EOFException();
				if (nextReady.hasError()) throw nextReady.getError();
				sp = nextReady;
			}
			if (full && !endReached) bufferize();
			if (chars != null) break;
			sp.block(0);
		}
		char c = chars.get();
		if (!chars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
			}
			if (full && !endReached) bufferize();
		}
		return c;
	}
	
	@Override
	public int readSync(char[] buf, int offset, int length) throws IOException {
		if (length <= 0)
			return 0;
		int done = 0;
		if (back != -1) {
			buf[offset++] = (char)back;
			back = -1;
			length--;
			if (length == 0)
				return 1;
			done = 1;
		}
		while (chars == null) {
			boolean full;
			SynchronizationPoint<IOException> sp;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
				sp = nextReady;
				if (chars == null && endReached) {
					if (done > 0) return done;
					return -1;
				}
			}
			if (full && !endReached) bufferize();
			if (chars != null) break;
			sp.block(0);
			if (sp.hasError()) throw sp.getError();
			if (sp.isCancelled()) throw IO.error(sp.getCancelEvent());
		}
		int len = length;
		if (len > chars.remaining()) len = chars.remaining();
		chars.get(buf, offset, len);
		if (!chars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
			}
			if (full && !endReached) bufferize();
		}
		return len + done;
	}
	
	@Override
	public int readAsync() throws IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		if (chars == null) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
				if (chars == null && endReached) return -1;
				if (nextReady.hasError()) throw nextReady.getError();
			}
			if (full && !endReached) bufferize();
			if (chars == null) return -2;
		}
		char c = chars.get();
		if (!chars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				chars = ready.pollFirst();
			}
			if (full && !endReached) bufferize();
		}
		return c;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(char[] buf, int off, int len) {
		if (len <= 0)
			return new AsyncWork<>(Integer.valueOf(0), null);
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		new Task.Cpu<Void, NoException>("BufferedReadableCharacterStream.readAsync", input.getPriority()) {
			@Override
			public Void run() {
				int done = 0;
				int offset = off;
				int length = len;
				if (back != -1) {
					buf[offset++] = (char)back;
					back = -1;
					length--;
					if (length == 0) {
						result.unblockSuccess(Integer.valueOf(1));
						return null;
					}
					done = 1;
				}
				if (chars == null) {
					boolean full;
					SynchronizationPoint<IOException> sp;
					synchronized (ready) {
						full = ready.isFull();
						chars = ready.pollFirst();
						sp = nextReady;
						if (chars == null && endReached) {
							if (done > 0)
								result.unblockSuccess(Integer.valueOf(done));
							else
								result.unblockSuccess(Integer.valueOf(-1));
							return null;
						}
					}
					if (full && !endReached) bufferize();
					if (chars == null) {
						int don = done;
						readAsync(buf, offset + done, length - done).listenInline(
							(nb) -> {
								result.unblockSuccess(Integer.valueOf(don + nb.intValue()));
							},
							result
						);
						return null;
					}
					if (sp.hasError()) {
						result.error(sp.getError());
						return null;
					}
					if (sp.isCancelled()) {
						result.cancel(sp.getCancelEvent());
						return null;
					}
				}
				int len = length;
				if (len > chars.remaining()) len = chars.remaining();
				chars.get(buf, offset, len);
				if (!chars.hasRemaining()) {
					boolean full;
					synchronized (ready) {
						full = ready.isFull();
						chars = ready.pollFirst();
					}
					if (full && !endReached) bufferize();
				}
				result.unblockSuccess(Integer.valueOf(len + done));
				return null;
			}
		}.startOn(canStartReading(), true);
		return result;
	}
	
	@Override
	public void close() {
		try { input.close(); }
		catch (Throwable t) { /* ignore */ }
		bytes = null;
		chars = null;
		ready = null;
		decoder = null;
		synchronized (ready) {
			nextReady.cancel(new CancelException("Closed"));
		}
	}
	
	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		bytes = null;
		chars = null;
		ready = null;
		decoder = null;
		synchronized (ready) {
			nextReady.cancel(new CancelException("Closed"));
		}
		return input.closeAsync();
	}
}
