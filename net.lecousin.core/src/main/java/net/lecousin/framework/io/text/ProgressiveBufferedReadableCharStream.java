package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.UnprotectedString;

/**
 * Buffered readable character stream.
 * The decoding is done in a separate task, as usual in buffered implementations.
 * The difference with other implementations is that when a character is needed and no more available decoded characters,
 * the decoding task makes a buffer available as soon as possible, even if the buffer is not fully decoded, making characters
 * available sooner.
 */
public class ProgressiveBufferedReadableCharStream extends ConcurrentCloseable<IOException> implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public ProgressiveBufferedReadableCharStream(Decoder decoder, int bufferSize, int maxBuffers) {
		this(decoder, bufferSize, maxBuffers, null);
	}
	
	/** Constructor. */
	public ProgressiveBufferedReadableCharStream(Decoder decoder, int bufferSize, int maxBuffers, CharBuffer firstChars) {
		if (maxBuffers > 100) maxBuffers = 100;
		this.decoder = decoder;
		this.bufferSize = bufferSize;
		this.buffers = new Buffer[maxBuffers];
		if (firstChars != null) {
			Buffer b = new Buffer();
			b.chars = new char[bufferSize];
			b.pos = 0;
			b.length = firstChars.remaining();
			if (b.length > bufferSize) b.length = bufferSize;
			firstChars.get(b.chars, 0, b.length);
			buffers[0] = b;
			currentBufferIndex = 0;
			currentBuffer = b;
			for (int i = 1; firstChars.hasRemaining() && i < maxBuffers; ++i) {
				b = new Buffer();
				b.chars = new char[bufferSize];
				b.pos = 0;
				b.length = firstChars.remaining();
				if (b.length > bufferSize) b.length = bufferSize;
				firstChars.get(b.chars, 0, b.length);
				buffers[i] = b;
				lastBufferReady = (byte)i;
			}
			if (lastBufferReady != -1) firstBufferReady = 1;
			if (firstChars.hasRemaining())
				throw new IllegalArgumentException("firstChars is too large to fit in buffers: bufferSize is "
					+ bufferSize + " and maxBuffers is " + maxBuffers + ", firstChars contains "
					+ firstChars.remaining() + " additional characters");
		}
		taskFillBuffer = new TaskFillBuffer();
		taskFillBuffer.start(); // TODO on io ready
	}
	
	private static class Buffer {
		char[] chars;
		int pos;
		int length;
	}
	
	private Decoder decoder;
	private int bufferSize;
	private byte currentBufferIndex = -1;
	private Buffer currentBuffer;
	private Buffer[] buffers;
	private byte firstBufferReady = -1;
	private byte lastBufferReady = -1;
	private Async<NoException> iNeedABuffer = null;
	private boolean eofReached = false;
	private TaskFillBuffer taskFillBuffer;
	private MutableBoolean interruptFillBuffer = new MutableBoolean(false);
	private IOException error = null;
	private int back = -1;
	private byte priority = Task.PRIORITY_NORMAL;

	@Override
	public void back(char c) {
		back = c;
	}
	
	/** Return true if eof is reached. */
	private boolean nextBuffer() throws IOException {
		synchronized (buffers) {
			if (error != null) throw error;
			if (iNeedABuffer == null || iNeedABuffer.isDone()) {
				iNeedABuffer = null;
				currentBufferIndex = -1;
				if (firstBufferReady != -1) {
					currentBufferIndex = firstBufferReady;
					currentBuffer = buffers[currentBufferIndex];
					if (firstBufferReady == lastBufferReady) {
						firstBufferReady = -1;
						lastBufferReady = -1;
					} else {
						firstBufferReady++;
						if (firstBufferReady == buffers.length)
							firstBufferReady = 0;
					}
					if (taskFillBuffer == null && !eofReached) {
						// we took a buffer, the task can start again
						taskFillBuffer = new TaskFillBuffer();
						taskFillBuffer.start();
					}
				} else if (eofReached) {
					return true;
				} else {
					iNeedABuffer = new Async<>();
					interruptFillBuffer.set(true);
				}
			}
		}
		return false;
	}

	@Override
	@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
	public char read() throws IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		do {
			if (currentBufferIndex != -1 && currentBuffer.pos < currentBuffer.length)
				return currentBuffer.chars[currentBuffer.pos++];
			if (nextBuffer())
				throw new EOFException();
			if (currentBufferIndex != -1)
				return currentBuffer.chars[currentBuffer.pos++];
			iNeedABuffer.block(0);
		} while (true);
	}

	@Override
	@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
	public int readSync(char[] buf, int offset, int length) throws IOException {
		if (length <= 0) return 0;
		int done;
		if (back != -1) {
			buf[offset++] = (char)back;
			back = -1;
			if (--length == 0)
				return 1;
			done = 1;
		} else {
			done = 0;
		}
		do {
			if (currentBufferIndex != -1) {
				int len = currentBuffer.length - currentBuffer.pos;
				if (len > 0) {
					if (len > length) len = length;
					System.arraycopy(currentBuffer.chars, currentBuffer.pos, buf, offset, len);
					currentBuffer.pos += len;
					return len + done;
				}
			}
			if (nextBuffer())
				return done > 0 ? done : -1;
			if (currentBufferIndex != -1) {
				int len = currentBuffer.length - currentBuffer.pos;
				if (len > length) len = length;
				System.arraycopy(currentBuffer.chars, currentBuffer.pos, buf, offset, len);
				currentBuffer.pos += len;
				return len + done;
			}
			iNeedABuffer.block(0);
		} while (true);
	}
	

	@Override
	@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
	public IAsync<IOException> canStartReading() {
		if (back != -1)
			return new Async<>(true);
		if (error != null)
			return new Async<>(error);
		if (currentBufferIndex != -1 && currentBuffer.pos < currentBuffer.length)
			return new Async<>(true);
		synchronized (buffers) {
			if (error != null)
				return new Async<>(error);
			if (iNeedABuffer != null && !iNeedABuffer.isDone()) {
				Async<IOException> sp = new Async<>();
				iNeedABuffer.onDone(() -> {
					if (error != null) sp.error(error);
					else sp.unblock();
				});
				return sp;
			}
			iNeedABuffer = null;
			currentBufferIndex = -1;
			if (firstBufferReady != -1) {
				currentBufferIndex = firstBufferReady;
				currentBuffer = buffers[currentBufferIndex];
				if (firstBufferReady == lastBufferReady) {
					firstBufferReady = -1;
					lastBufferReady = -1;
				} else {
					firstBufferReady++;
					if (firstBufferReady == buffers.length)
						firstBufferReady = 0;
				}
				if (taskFillBuffer == null && !eofReached) {
					// we took a buffer, the task can start again
					taskFillBuffer = new TaskFillBuffer();
					taskFillBuffer.start();
				}
			} else if (eofReached) {
				return new Async<>(true);
			} else {
				iNeedABuffer = new Async<>();
				interruptFillBuffer.set(true);
			}
		}
		if (currentBufferIndex != -1)
			return new Async<>(true);
		Async<IOException> sp = new Async<>();
		iNeedABuffer.onDone(() -> {
			if (error != null) sp.error(error);
			else sp.unblock();
		});
		return sp;
	}

	@Override
	@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
	public int readAsync() throws IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		do {
			if (currentBufferIndex != -1 && currentBuffer.pos < currentBuffer.length)
				return currentBuffer.chars[currentBuffer.pos++];
			if (nextBuffer())
				return -1;
			if (currentBufferIndex != -1)
				return currentBuffer.chars[currentBuffer.pos++];
			if (!iNeedABuffer.isDone())
				return -2;
		} while (true);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(char[] buf, int offset, int length) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		readAsync(buf, offset, length, result);
		return result;
	}
	
	private void readAsync(char[] buf, int off, int l, AsyncSupplier<Integer, IOException> result) {
		new Task.Cpu<Void, NoException>("readAsync", priority) {
			@Override
			@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
			public Void run() {
				int offset = off;
				int length = l;
				int done;
				if (back != -1) {
					buf[offset++] = (char)back;
					back = -1;
					if (--length == 0) {
						result.unblockSuccess(Integer.valueOf(1));
						return null;
					}
					done = 1;
				} else {
					done = 0;
				}
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					if (len > 0) {
						if (len > length) len = length;
						System.arraycopy(currentBuffer.chars, currentBuffer.pos, buf, offset, len);
						currentBuffer.pos += len;
						result.unblockSuccess(Integer.valueOf(len + done));
						return null;
					}
				}
				if (done == 1) {
					result.unblockSuccess(Integer.valueOf(1));
					return null;
				}
				try {
					if (nextBuffer()) {
						result.unblockSuccess(Integer.valueOf(-1));
						return null;
					}
				} catch (IOException e) {
					result.error(error);
					return null;
				}
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					if (len > length) len = length;
					System.arraycopy(currentBuffer.chars, currentBuffer.pos, buf, offset, len);
					currentBuffer.pos += len;
					result.unblockSuccess(Integer.valueOf(len));
					return null;
				}
				iNeedABuffer.onDone(() -> readAsync(buf, off, l, result));
				return null;
			}
		}.start();
	}

	@Override
	public AsyncSupplier<UnprotectedString, IOException> readNextBufferAsync() {
		AsyncSupplier<UnprotectedString, IOException> result = new AsyncSupplier<>();
		readNextBufferAsync(result);
		return result;
	}

	private void readNextBufferAsync(AsyncSupplier<UnprotectedString, IOException> result) {
		new Task.Cpu<Void, NoException>("readNextBufferAsync", priority) {
			@Override
			@SuppressWarnings("squid:S2259") // iNeedABuffer cannot be null because currentBufferIndex != -1
			public Void run() {
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					if (len > 0) {
						UnprotectedString s = new UnprotectedString(len + (back != -1 ? 1 : 0));
						if (back != -1) {
							s.append((char)back);
							back = -1;
						}
						s.append(currentBuffer.chars, currentBuffer.pos, len);
						result.unblockSuccess(s);
						return null;
					} else if (back != -1) {
						UnprotectedString s = new UnprotectedString((char)back);
						back = -1;
						result.unblockSuccess(s);
						return null;
					}
				}
				try {
					if (nextBuffer()) {
						if (back != -1) {
							char c = (char)back;
							back = -1;
							result.unblockSuccess(new UnprotectedString(c));
						} else {
							result.unblockSuccess(null);
						}
						return null;
					}
				} catch (IOException e) {
					result.error(error);
					return null;
				}
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					UnprotectedString s = new UnprotectedString(len);
					s.append(currentBuffer.chars, currentBuffer.pos, len);
					currentBuffer.pos += len;
					result.unblockSuccess(s);
					return null;
				}
				iNeedABuffer.onDone(() -> readNextBufferAsync(result));
				return null;
			}
		}.start();
	}

	
	private class TaskFillBuffer extends Task.Cpu<Void, NoException> {
		private TaskFillBuffer() {
			super("Fill buffers", priority);
		}

		private Buffer buffer = null;
		private byte myBuffer = -1;
		private int lastNb = 0;
		
		@Override
		@SuppressWarnings("squid:S1854") // not useless assignment, useful for garbage collection
		public Void run() throws CancelException {
			do {
				if (buffer == null && !getNextBuffer())
					return null; // full
				int nb = decode();
				if (nb == -3)
					return null;
				if (isCancelling())
					return null;
				if (nb == -2) {
					needMoreData();
					return null;
				}
				if (nb == -1) {
					noMoreCharacter();
					return null;
				}
				lastNb = nb;
				buffer.length += nb;
				synchronized (buffers) {
					if (buffer.length == bufferSize ||
						(buffer.length > 0 && iNeedABuffer != null && !iNeedABuffer.isDone())) {
						lastBufferReady = myBuffer;
						if (firstBufferReady == -1) firstBufferReady = myBuffer;
						myBuffer = -1;
						buffer = null;
						interruptFillBuffer.set(false);
						if (iNeedABuffer != null)
							iNeedABuffer.unblock();
					}
				}
			} while (!isClosing());
			return null;
		}
		
		private boolean getNextBuffer() {
			synchronized (buffers) {
				if (firstBufferReady == -1) {
					myBuffer = (byte) (currentBufferIndex + 1);
					if (myBuffer == buffers.length) myBuffer = 0;
				} else {
					myBuffer = (byte) (lastBufferReady + 1);
					if (myBuffer == buffers.length) myBuffer = 0;
					if (myBuffer == firstBufferReady || myBuffer == currentBufferIndex) {
						// full => stop the task
						taskFillBuffer = null;
						return false;
					}
				}
			}
			buffer = buffers[myBuffer];
			if (buffer == null) {
				// TODO reuse previous buffer if available
				buffer = new Buffer();
				buffer.chars = new char[bufferSize];
				buffers[myBuffer] = buffer;
			}
			buffer.pos = 0;
			buffer.length = 0;
			return true;
		}
		
		private int decode() throws CancelException {
			int min = lastNb < bufferSize / 10 ? bufferSize / 10 : 1;
			try {
				return decoder.decode(buffer.chars, buffer.length, bufferSize - buffer.length, interruptFillBuffer, min);
			} catch (IOException e) {
				synchronized (buffers) {
					error = e;
					if (iNeedABuffer != null)
						iNeedABuffer.unblock();
				}
				return -3;
			}
		}
		
		private void needMoreData() {
			synchronized (buffers) {
				if (buffer.length > 0) {
					lastBufferReady = myBuffer;
					if (firstBufferReady == -1) firstBufferReady = myBuffer;
					myBuffer = -1;
					buffer = null;
					interruptFillBuffer.set(false);
					if (iNeedABuffer != null)
						iNeedABuffer.unblock();
				}
				taskFillBuffer = new TaskFillBuffer();
			}
			decoder.canDecode().thenStart(taskFillBuffer, true);
		}
		
		private void noMoreCharacter() {
			synchronized (buffers) {
				if (buffer.length > 0) {
					lastBufferReady = myBuffer;
					if (firstBufferReady == -1) firstBufferReady = myBuffer;
					myBuffer = -1;
					buffer = null;
					interruptFillBuffer.set(false);
				}
				eofReached = true;
				if (iNeedABuffer != null)
					iNeedABuffer.unblock();
			}
		}
	}
	

	@Override
	public boolean endReached() {
		synchronized (buffers) {
			if (back != -1) return false;
			if (firstBufferReady != -1) return false;
			if (currentBufferIndex != -1 && buffers[currentBufferIndex].pos < buffers[currentBufferIndex].length) return false;
			return eofReached;
		}
	}

	@Override
	public Charset getEncoding() {
		return decoder.getEncoding();
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public void setPriority(byte priority) {
		this.priority = priority;
	}

	@Override
	public String getDescription() {
		return "Buffered character stream"; // TODO better, taken from decoder
	}

	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		interruptFillBuffer.set(true);
		TaskFillBuffer task = taskFillBuffer;
		if (task != null) {
			task.cancel(new CancelException("Closing"));
			Async<IOException> sp = new Async<>();
			task.getOutput().onDone(() -> decoder.closeAsync().onDone(sp));
			return sp;
		}
		return decoder.closeAsync();
	}

	@Override
	protected void closeResources(Async<IOException> ondone) {
		decoder = null;
		buffers = null;
		ondone.unblock();
	}
	
}
