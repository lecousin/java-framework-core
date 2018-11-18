package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
public class ProgressiveBufferedReadableCharStream extends ConcurrentCloseable implements ICharacterStream.Readable.Buffered {

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
					+ bufferSize + " and maxBuffers is " + maxBuffers);
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
	private SynchronizationPoint<NoException> iNeedABuffer = null;
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

	@Override
	public char read() throws EOFException, IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		do {
			if (currentBufferIndex != -1) {
				if (currentBuffer.pos < currentBuffer.length)
					return currentBuffer.chars[currentBuffer.pos++];
			}
			synchronized (buffers) {
				if (error != null) throw error;
				if (iNeedABuffer == null || iNeedABuffer.isUnblocked()) {
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
					} else if (eofReached)
						throw new EOFException();
					else {
						iNeedABuffer = new SynchronizationPoint<>();
						interruptFillBuffer.set(true);
					}
				}
			}
			if (currentBufferIndex != -1)
				return currentBuffer.chars[currentBuffer.pos++];
			iNeedABuffer.block(0);
		} while (true);
	}

	@Override
	public int readSync(char[] buf, int offset, int length) throws IOException {
		int done;
		if (back != -1) {
			buf[offset++] = (char)back;
			back = -1;
			if (--length == 0)
				return 1;
			done = 1;
		} else
			done = 0;
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
			synchronized (buffers) {
				if (error != null) throw error;
				if (iNeedABuffer == null || iNeedABuffer.isUnblocked()) {
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
					} else if (eofReached)
						return done > 0 ? done : -1;
					else {
						iNeedABuffer = new SynchronizationPoint<>();
						interruptFillBuffer.set(true);
					}
				}
			}
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
	public ISynchronizationPoint<IOException> canStartReading() {
		if (back != -1)
			return new SynchronizationPoint<>(true);
		if (error != null)
			return new SynchronizationPoint<>(error);
		if (currentBufferIndex != -1) {
			if (currentBuffer.pos < currentBuffer.length)
				return new SynchronizationPoint<>(true);
		}
		synchronized (buffers) {
			if (error != null)
				return new SynchronizationPoint<>(error);
			if (iNeedABuffer != null && !iNeedABuffer.isUnblocked()) {
				SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
				iNeedABuffer.listenInline(() -> {
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
			} else if (eofReached)
				return new SynchronizationPoint<>(true);
			else {
				iNeedABuffer = new SynchronizationPoint<>();
				interruptFillBuffer.set(true);
			}
		}
		if (currentBufferIndex != -1)
			return new SynchronizationPoint<>(true);
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		iNeedABuffer.listenInline(() -> {
			if (error != null) sp.error(error);
			else sp.unblock();
		});
		return sp;
	}

	@Override
	public int readAsync() throws IOException {
		if (back != -1) {
			char c = (char)back;
			back = -1;
			return c;
		}
		do {
			if (currentBufferIndex != -1) {
				if (currentBuffer.pos < currentBuffer.length)
					return currentBuffer.chars[currentBuffer.pos++];
			}
			synchronized (buffers) {
				if (error != null) throw error;
				if (iNeedABuffer == null || iNeedABuffer.isUnblocked()) {
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
					} else if (eofReached)
						return -1;
					else {
						iNeedABuffer = new SynchronizationPoint<>();
						interruptFillBuffer.set(true);
					}
				}
			}
			if (currentBufferIndex != -1)
				return currentBuffer.chars[currentBuffer.pos++];
			if (!iNeedABuffer.isUnblocked())
				return -2;
		} while (true);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(char[] buf, int offset, int length) {
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		readAsync(buf, offset, length, result);
		return result;
	}
	
	private void readAsync(char[] buf, int off, int l, AsyncWork<Integer, IOException> result) {
		new Task.Cpu<Void, NoException>("readAsync", priority) {
			@Override
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
				} else
					done = 0;
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
				synchronized (buffers) {
					if (error != null) {
						result.error(error);
						return null;
					}
					if (iNeedABuffer == null || iNeedABuffer.isUnblocked()) {
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
							result.unblockSuccess(Integer.valueOf(-1));
							return null;
						} else {
							iNeedABuffer = new SynchronizationPoint<>();
							interruptFillBuffer.set(true);
						}
					}
				}
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					if (len > length) len = length;
					System.arraycopy(currentBuffer.chars, currentBuffer.pos, buf, offset, len);
					currentBuffer.pos += len;
					result.unblockSuccess(Integer.valueOf(len));
					return null;
				}
				iNeedABuffer.listenInline(() -> {
					readAsync(buf, off, l, result);
				});
				return null;
			}
		}.start();
	}

	@Override
	public AsyncWork<UnprotectedString, IOException> readNextBufferAsync() {
		AsyncWork<UnprotectedString, IOException> result = new AsyncWork<>();
		readNextBufferAsync(result);
		return result;
	}

	private void readNextBufferAsync(AsyncWork<UnprotectedString, IOException> result) {
		new Task.Cpu<Void, NoException>("readNextBufferAsync", priority) {
			@Override
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
				synchronized (buffers) {
					if (error != null) {
						result.error(error);
						return null;
					}
					if (iNeedABuffer == null || iNeedABuffer.isUnblocked()) {
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
							result.unblockSuccess(null);
							return null;
						} else {
							iNeedABuffer = new SynchronizationPoint<>();
							interruptFillBuffer.set(true);
						}
					}
				}
				if (currentBufferIndex != -1) {
					int len = currentBuffer.length - currentBuffer.pos;
					UnprotectedString s = new UnprotectedString(len);
					s.append(currentBuffer.chars, currentBuffer.pos, len);
					currentBuffer.pos += len;
					result.unblockSuccess(s);
					return null;
				}
				iNeedABuffer.listenInline(() -> {
					readNextBufferAsync(result);
				});
				return null;
			}
		}.start();
	}

	
	private class TaskFillBuffer extends Task.Cpu<Void, NoException> {
		private TaskFillBuffer() {
			super("Fill buffers", priority);
		}
		
		@Override
		public Void run() throws CancelException {
			Buffer buffer = null;
			byte myBuffer = -1;
			int lastNb = 0;
			do {
				if (buffer == null) {
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
								return null;
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
				}
				int min = lastNb < bufferSize / 10 ? bufferSize / 10 : 1;
				int nb;
				try { nb = decoder.decode(buffer.chars, buffer.pos, bufferSize - buffer.pos, interruptFillBuffer, min); }
				catch (IOException e) {
					synchronized (buffers) {
						error = e;
						if (iNeedABuffer != null)
							iNeedABuffer.unblock();
					}
					return null;
				}
				if (nb == -2) {
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
					decoder.canDecode().listenAsync(taskFillBuffer, true);
					return null;
				}
				if (nb == -1) {
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
					return null;
				}
				lastNb = nb;
				buffer.length += nb;
				synchronized (buffers) {
					if (buffer.length == bufferSize ||
						(buffer.length > 0 && iNeedABuffer != null && !iNeedABuffer.isUnblocked())) {
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
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		interruptFillBuffer.set(true);
		TaskFillBuffer task = taskFillBuffer;
		if (task != null) {
			task.cancel(new CancelException("Closing"));
			SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
			task.getOutput().listenInline(() -> { decoder.closeAsync().listenInline(sp); });
			return sp;
		}
		return decoder.closeAsync();
	}

	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		decoder = null;
		buffers = null;
		ondone.unblock();
	}
	
}
