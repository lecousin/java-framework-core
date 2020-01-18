package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.encoding.charset.CharacterDecoder;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.io.data.RawCharBuffer;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.text.IString;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Implement a buffered readable character stream from a readable IO. */
public class BufferedReadableCharacterStream extends ConcurrentCloseable<IOException> implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public BufferedReadableCharacterStream(IO.Readable input, Charset charset, int bufferSize, int maxBuffers) {
		this(input, CharacterDecoder.get(charset, bufferSize), bufferSize, maxBuffers);
	}

	/** Constructor. */
	public BufferedReadableCharacterStream(IO.Readable input, CharacterDecoder decoder, int bufferSize, int maxBuffers) {
		this(input, decoder, bufferSize, maxBuffers, null, null);
	}

	/** Constructor. */
	public BufferedReadableCharacterStream(
		IO.Readable input, Charset charset, int bufferSize, int maxBuffers,
		ByteBuffer initBytes, Chars.Readable initChars
	) {
		this(input, CharacterDecoder.get(charset, bufferSize), bufferSize, maxBuffers, initBytes, initChars);
	}
	
	/** Constructor. */
	public BufferedReadableCharacterStream(
		IO.Readable input, CharacterDecoder decoder, int bufferSize, int maxBuffers,
		ByteBuffer initBytes, Chars.Readable initChars
	) {
		this.input = input;
		this.decoder = decoder;
		if (bufferSize < 64) bufferSize = 64;
		ready = new TurnArray<>(maxBuffers);
		cache = ByteArrayCache.getInstance();
		this.bufferSize = bufferSize;
		currentChars = initChars != null && initChars.hasRemaining() ? initChars : null;
		// start decoding
		if (initBytes != null && initBytes.hasRemaining()) {
			RawByteBuffer rb = new RawByteBuffer(initBytes);
			AsyncSupplier<Integer,IOException> readTask = new AsyncSupplier<>(Integer.valueOf(rb.remaining()), null);
			DecodeTask decode = new DecodeTask(readTask, rb.array, rb.currentOffset, rb.remaining());
			operation(decode).startOn(readTask, true);
		} else {
			if (input instanceof IO.Readable.Buffered)
				((IO.Readable.Buffered)input).canStartReading().onDone(this::bufferize);
			else
				bufferize();
		}
	}
	
	private IO.Readable input;
	private CharacterDecoder decoder;
	
	private TurnArray<Chars.Readable> ready; 
	private ByteArrayCache cache;
	private int bufferSize;
	private Chars.Readable currentChars;
	private boolean endReached = false;
	private int back = -1;
	private Async<IOException> nextReady = new Async<>();
	
	@Override
	public Async<IOException> canStartReading() {
		synchronized (ready) {
			if (ready.isEmpty())
				return nextReady;
			return new Async<>(true);
		}
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		synchronized (ready) {
			nextReady.cancel(new CancelException("Closed"));
		}
		return input.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		currentChars = null;
		decoder = null;
		ready = null;
		input = null;
		ondone.unblock();
	}
	
	@Override
	public String getDescription() { return input.getSourceDescription(); }
	
	@Override
	public Charset getEncoding() {
		return decoder.getEncoding();
	}
	
	@Override
	public byte getPriority() {
		return input.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		input.setPriority(priority);
	}
	
	private void bufferize() {
		byte[] buf = cache.get(bufferSize, true);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		AsyncSupplier<Integer,IOException> readTask = input.readFullyAsync(bb);
		DecodeTask decode = new DecodeTask(readTask, buf, 0, buf.length);
		operation(decode).startOn(readTask, true);
	}
	
	private class DecodeTask extends Task.Cpu<Void,NoException> {
		
		private DecodeTask(AsyncSupplier<Integer,IOException> readTask, byte[] buf, int bufOffset, int expectedRead) {
			super("Decode character stream", input.getPriority());
			this.readTask = readTask;
			this.buf = buf;
			this.bufOffset = bufOffset;
			this.expectedRead = expectedRead;
		}
		
		private AsyncSupplier<Integer,IOException> readTask;
		private byte[] buf;
		private int bufOffset;
		private int expectedRead;
		
		@Override
		@SuppressWarnings("squid:S1696") // NullPointerException
		public Void run() {
			if (nextReady.isCancelled()) return null; // closed
			if (readTask.isCancelled()) {
				if (decoder == null)
					return null; // closed
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
			if (nextReady.isCancelled()) return null; // closed
			try {
				decode();
				return null;
			} catch (NullPointerException e) {
				// closed
				return null;
			} catch (Exception t) {
				if (!nextReady.isDone())
					synchronized (ready) {
						nextReady.error(IO.error(t));
					}
				LCCore.getApplication().getDefaultLogger().error("Error while buffering", t);
				return null;
			}
		}
		
		private void decode() {
			int nb = readTask.getResult().intValue();
			boolean end;
			if (nb < expectedRead) {
				input.closeAsync();
				end = true;
			} else {
				end = false;
			}
			if (nextReady.isCancelled()) return; // closed
			Chars.Readable chars;
			if (nb > 0) {
				RawByteBuffer in = new RawByteBuffer(buf, bufOffset, nb);
				chars = decoder.decode(in);
			} else {
				chars = null;
			}
			if (nextReady.isCancelled()) return; // closed
			if (chars != null && !chars.hasRemaining()) chars = null;
			Chars.Readable endChars = end ? decoder.flush() : null;
			Async<IOException> sp = null;
			boolean full = false;
			synchronized (ready) {
				if (chars == null && endChars == null) {
					if (end) {
						endReached = true;
						sp = nextReady;
						nextReady = new Async<>(true);
					}
				} else {
					if (chars != null) {
						if (endChars != null) {
							if (ready.getNbAvailableSlots() >= 2) {
								ready.addLast(chars);
								ready.addLast(endChars);
							} else {
								ready.addLast(new CompositeChars.Readable(chars, endChars));
							}
						} else {
							ready.addLast(chars);
						}
					} else {
						ready.addLast(endChars);
					}
					full = ready.isFull();
					sp = nextReady;
					nextReady = new Async<>(end);
					endReached = end;
				}
			}
			if (sp == null) {
				bufferize();
				return;
			}
			sp.unblock();
			if (!full && !endReached)
				bufferize();
		}
	}
	
	private void pollNextBuffer() {
		currentChars = ready.pollFirst();
	}
	
	@Override
	public boolean endReached() {
		if (endReached && currentChars == null) {
			synchronized (ready) {
				if (endReached && currentChars == null && ready.isEmpty())
					return true;
			}
		}
		return false;
	}
	
	@Override
	public void back(char c) {
		back = c;
	}
	
	@Override
	public char read() throws IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		while (currentChars == null) {
			boolean full;
			Async<IOException> sp;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
				if (currentChars == null && endReached) throw new EOFException();
				if (nextReady.hasError()) throw nextReady.getError();
				sp = nextReady;
			}
			if (full && !endReached) bufferize();
			if (currentChars != null) break;
			sp.block(0);
		}
		char c = currentChars.get();
		if (!currentChars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
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
		while (currentChars == null) {
			boolean full;
			Async<IOException> sp;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
				sp = nextReady;
				if (currentChars == null && endReached) {
					if (done > 0) return done;
					return -1;
				}
			}
			if (full && !endReached) bufferize();
			if (currentChars != null) break;
			sp.block(0);
			if (sp.hasError()) throw sp.getError();
			if (sp.isCancelled()) throw IO.error(sp.getCancelEvent());
		}
		int len = Math.min(length, currentChars.remaining());
		currentChars.get(buf, offset, len);
		if (!currentChars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
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
		if (currentChars == null) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
				if (currentChars == null && endReached) return -1;
				if (nextReady.hasError()) throw nextReady.getError();
			}
			if (full && !endReached) bufferize();
			if (currentChars == null) return -2;
		}
		char c = currentChars.get();
		if (!currentChars.hasRemaining()) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
			}
			if (full && !endReached) bufferize();
		}
		return c;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(char[] buf, int off, int len) {
		if (len <= 0)
			return new AsyncSupplier<>(Integer.valueOf(0), null);
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		operation(new ReadAsyncTask(buf, off, len, result)).startOn(canStartReading(), true);
		return result;
	}
	
	private class ReadAsyncTask extends Task.Cpu<Void, NoException> {
		
		private ReadAsyncTask(char[] buf, int offset, int length, AsyncSupplier<Integer, IOException> result) {
			super("BufferedReadableCharacterStream.readAsync", input.getPriority());
			this.buf = buf;
			this.offset = offset;
			this.length = length;
			this.readResult = result;
		}
		
		private char[] buf;
		private int offset;
		private int length;
		private AsyncSupplier<Integer, IOException> readResult;
		
		@Override
		public Void run() {
			int done = 0;
			
			if (back != -1) {
				buf[offset++] = (char)back;
				back = -1;
				length--;
				if (length == 0) {
					readResult.unblockSuccess(Integer.valueOf(1));
					return null;
				}
				done = 1;
			}
			
			if (currentChars == null && !getNextChars(done, readResult))
				return null;
			
			int len = Math.min(length, currentChars.remaining());
			currentChars.get(buf, offset, len);
			
			if (!currentChars.hasRemaining()) {
				boolean full;
				synchronized (ready) {
					full = ready.isFull();
					pollNextBuffer();
				}
				if (full && !endReached) bufferize();
			}
			
			readResult.unblockSuccess(Integer.valueOf(len + done));
			return null;
		}
		
		private boolean getNextChars(int done, AsyncSupplier<Integer, IOException> result) {
			boolean full;
			Async<IOException> sp;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
				sp = nextReady;
				if (currentChars == null && endReached) {
					if (done > 0)
						result.unblockSuccess(Integer.valueOf(done));
					else
						result.unblockSuccess(Integer.valueOf(-1));
					return false;
				}
			}
			if (full && !endReached) bufferize();
			if (currentChars == null) {
				if (sp.forwardIfNotSuccessful(result))
					return false;
				int don = done;
				readAsync(buf, offset + done, length - done).onDone(
					nb -> result.unblockSuccess(Integer.valueOf(don + nb.intValue())),
					result
				);
				return false;
			}
			return !sp.forwardIfNotSuccessful(result);
		}
	}
	
	@Override
	public AsyncSupplier<Chars.Readable, IOException> readNextBufferAsync() {
		AsyncSupplier<Chars.Readable, IOException> result = new AsyncSupplier<>();
		readNextBufferAsync(result);
		return result;
	}
	
	private void readNextBufferAsync(AsyncSupplier<Chars.Readable, IOException> result) {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			result.unblockSuccess(new RawCharBuffer(new char[] { c }));
			return;
		}
		if (currentChars == null) {
			boolean full;
			synchronized (ready) {
				full = ready.isFull();
				pollNextBuffer();
				if (currentChars == null && endReached) {
					result.unblockSuccess(null);
					return;
				}
				if (nextReady.hasError()) {
					result.error(nextReady.getError());
					return;
				}
			}
			if (full && !endReached) bufferize();
			if (currentChars == null) {
				canStartReading().thenStart(
				new Task.Cpu.FromRunnable("BufferedReadableCharacterStream.readNextBufferAsync", getPriority(),
					() -> readNextBufferAsync(result)), result);
				return;
			}
		}
		Chars.Readable buf = currentChars;
		boolean full;
		synchronized (ready) {
			full = ready.isFull();
			pollNextBuffer();
		}
		if (full && !endReached) bufferize();
		result.unblockSuccess(buf);
	}
	
	@Override
	public Chars.Readable readNextBuffer() throws IOException {
		try {
			return readNextBufferAsync().blockResult(0);
		} catch (CancelException e) {
			throw IO.errorCancelled(e);
		}
	}
	
	@Override
	public AsyncSupplier<Boolean, IOException> readUntilAsync(char endChar, IString string) {
		AsyncSupplier<Boolean, IOException> result = new AsyncSupplier<>();
		new Task.Cpu.FromRunnable("BufferedReadableCharacterStream.readUntil", getPriority(),
			() -> readUntil(endChar, string, result)).start();
		return result;
	}
	
	@Override
	public boolean readUntil(char endChar, IString string) throws IOException {
		AsyncSupplier<Boolean, IOException> result = new AsyncSupplier<>();
		readUntil(endChar, string, result);
		try {
			return result.blockResult(0).booleanValue();
		} catch (CancelException e) {
			throw IO.errorCancelled(e);
		}
	}
	
	private void readUntil(char endChar, IString string, AsyncSupplier<Boolean, IOException> result) {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			if (c == endChar) {
				result.unblockSuccess(Boolean.TRUE);
				return;
			}
			string.append(c);
		}
		do {
			if (currentChars == null) {
				boolean full;
				synchronized (ready) {
					full = ready.isFull();
					pollNextBuffer();
					if (currentChars == null && endReached) {
						result.unblockSuccess(Boolean.FALSE);
						return;
					}
					if (nextReady.hasError()) {
						result.error(nextReady.getError());
						return;
					}
				}
				if (full && !endReached) bufferize();
				if (currentChars == null) {
					canStartReading().thenStart(
					new Task.Cpu.FromRunnable("BufferedReadableCharacterStream.readUntil", getPriority(),
						() -> readUntil(endChar, string, result)), result);
					return;
				}
			}
			boolean found = searchCurrentChars(endChar, string);
			if (!currentChars.hasRemaining()) {
				boolean full;
				synchronized (ready) {
					full = ready.isFull();
					pollNextBuffer();
				}
				if (full && !endReached) bufferize();
			}
			if (found) {
				result.unblockSuccess(Boolean.TRUE);
				return;
			}
		} while (true);
	}
	
	private boolean searchCurrentChars(char endChar, IString string) {
		int r = currentChars.remaining();
		for (int i = 0; i < r; ++i) {
			if (currentChars.getForward(i) == endChar) {
				if (i == 0) {
					currentChars.moveForward(1);
				} else {
					currentChars.get(string, i);
					currentChars.moveForward(1);
				}
				return true;
			}
		}
		currentChars.get(string, currentChars.remaining());
		return false;
	}
}
