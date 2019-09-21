package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;

/**
 * IO that can read bytes backward.
 * It is buffered, so it implements ReadableByteStream to read forward.
 * A typical usage is when searching for a specific pattern starting from the end of an IO.
 */
public class BufferedReverseIOReading extends ConcurrentCloseable implements IO.ReadableByteStream {

	/** Create an IO capable to read bytes backward from a Seekable. */
	public <T extends IO.Readable.Seekable & IO.KnownSize> BufferedReverseIOReading(T io, int bufferSize) {
		this.io = io;
		buffer = new byte[bufferSize];
		io.getSizeAsync().listenInline(new AsyncWorkListener<Long, IOException>() {
			@Override
			public void ready(Long result) {
				operation(new Task.Cpu.FromRunnable("Read last buffer", io.getPriority(), () -> {
					synchronized (BufferedReverseIOReading.this) {
						fileSize = bufferPosInFile = result.longValue();
						readBufferBack();
					}
				}).start().getOutput());
			}
			
			@Override
			public void error(IOException error) {
				BufferedReverseIOReading.this.error = error;
			}
			
			@Override
			public void cancelled(CancelException event) {
				// nothing
			}
		});
	}
	
	private IO.Readable.Seekable io;
	private byte[] buffer;
	private IOException error = null;
	private long fileSize;
	private long bufferPosInFile;
	private int posInBuffer = 0;
	private int minInBuffer = 0;
	private int maxInBuffer = 0;
	private SynchronizationPoint<IOException> canRead = new SynchronizationPoint<>();
	private AsyncWork<Integer,IOException> currentRead = null;
	
	@Override
	public SynchronizationPoint<IOException> canStartReading() {
		return canRead;
	}
	
	/** Try to stop any pending read operation on the underlying IO, and wait them to end. */
	public void stop() {
		synchronized (this) {
			if (currentRead != null)
				currentRead.unblockCancel(new CancelException("BufferedReverseIO stopped"));
		}
		canRead.block(0);
	}
	
	public long getSize() {
		return fileSize;
	}
	
	@Override
	public byte getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(byte priority) { io.setPriority(priority); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public TaskManager getTaskManager() { return Threading.getCPUTaskManager(); }
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		synchronized (this) {
			if (currentRead != null)
				currentRead.unblockCancel(new CancelException("BufferedReverseIO stopped"));
		}
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		buffer = null;
		io = null;
		if (!canRead.isUnblocked())
			canRead.cancel(new CancelException("IO Closed"));
		ondone.unblock();
	}
	
	/** Read a byte backward. */
	public int readReverse() throws IOException {
		int b;
		do {
			canRead.block(0);
			synchronized (this) {
				if (error != null) throw error;
				if (io == null) throw new ClosedChannelException();
				// check if we reached the beginning of the file
				if (bufferPosInFile == 0 && posInBuffer == minInBuffer) return -1;
				if (posInBuffer == minInBuffer) {
					// we are reading faster than buffering
					if (currentRead != null)
						canRead.restart();
					else
						readBufferBack();
					b = -1;
				} else {
					b = buffer[--posInBuffer] & 0xFF;
					if (currentRead == null && posInBuffer < buffer.length / 2 && bufferPosInFile > 0)
						readBufferBack();
				}
			}
		} while (b == -1);
		return b;
	}
	
	@Override
	public int read() throws IOException {
		int b;
		do {
			canRead.block(0);
			synchronized (this) {
				if (error != null) throw error;
				if (io == null) throw new ClosedChannelException();
				// check if we reached the end of the file
				if (bufferPosInFile + (maxInBuffer - minInBuffer) == fileSize && posInBuffer == maxInBuffer) return -1;
				if (posInBuffer == maxInBuffer) {
					// we are reading faster than buffering
					if (currentRead != null)
						canRead.restart();
					else
						readBufferForward();
					b = -1;
				} else {
					b = buffer[posInBuffer++] & 0xFF;
					if (currentRead == null &&
						posInBuffer > buffer.length / 2 &&
						bufferPosInFile + (maxInBuffer - minInBuffer) < fileSize)
						readBufferForward();
				}
			}
		} while (b == -1);
		return b;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		do {
			canRead.block(0);
			synchronized (this) {
				if (error != null) throw error;
				if (io == null) throw new ClosedChannelException();
				// check if we reached the end of the file
				if (bufferPosInFile + (maxInBuffer - minInBuffer) == fileSize && posInBuffer == maxInBuffer) return -1;
				if (posInBuffer == maxInBuffer) {
					// we are reading faster than buffering
					if (currentRead != null)
						canRead.restart();
					else
						readBufferForward();
					continue;
				}
				if (len > maxInBuffer - posInBuffer) len = maxInBuffer - posInBuffer;
				System.arraycopy(this.buffer, posInBuffer, buffer, offset, len);
				posInBuffer += len;
				if (currentRead == null &&
					posInBuffer > buffer.length / 2 &&
					bufferPosInFile + (maxInBuffer - minInBuffer) < fileSize)
					readBufferForward();
				return len;
			}
		} while (true);
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public int skip(int skip) throws IOException {
		int done = 0;
		do {
			canRead.block(0);
			synchronized (this) {
				if (error != null) throw error;
				if (io == null) throw new ClosedChannelException();
				// check if we reached the end of the file
				if (bufferPosInFile + (maxInBuffer - minInBuffer) == fileSize && posInBuffer == maxInBuffer) return done;
				if (posInBuffer == maxInBuffer) {
					// we are reading faster than buffering
					if (currentRead != null)
						canRead.restart();
					else
						readBufferForward();
					continue;
				}
				int len = skip - done;
				if (len > maxInBuffer - posInBuffer) len = maxInBuffer - posInBuffer;
				posInBuffer += len;
				done += len;
				if (currentRead == null &&
					posInBuffer > buffer.length / 2 &&
					bufferPosInFile + (maxInBuffer - minInBuffer) < fileSize)
					readBufferForward();
				if (done == skip) return done;
			}
		} while (true);
	}
	
	
	private void readBufferBack() {
		int len;
		if (posInBuffer > minInBuffer) {
			// we have posInBuffer-minInBuffer bytes remaining at the beginning
			// bytes which can be read are before those bytes
			len = buffer.length - (posInBuffer - minInBuffer);
			// we move them at the end of the buffer
			System.arraycopy(buffer, minInBuffer, buffer, len, posInBuffer - minInBuffer);
			// new minimum position in the buffer, until the read is done
			minInBuffer = len;
			// we start back at then end of the buffer
			posInBuffer = buffer.length;
			maxInBuffer = buffer.length;
		} else {
			len = buffer.length;
			minInBuffer = buffer.length;
			maxInBuffer = buffer.length;
			posInBuffer = buffer.length;
			canRead.restart();
		}
		// read the bytes
		int start;
		if (bufferPosInFile - len < 0) {
			start = (int)(len - bufferPosInFile);
			len = (int)bufferPosInFile;
		} else {
			start = 0;
		}
		ByteBuffer buf = ByteBuffer.wrap(buffer, start, len);
		currentRead = io.readFullyAsync(bufferPosInFile - len, buf);
		currentRead.listenAsync(operation(new Task.Cpu.FromRunnable("New buffer ready", io.getPriority(), () -> {
			synchronized (BufferedReverseIOReading.this) {
				if (!currentRead.isSuccessful()) {
					error = currentRead.getError();
				} else {
					int nb = currentRead.getResult().intValue();
					bufferPosInFile -= nb;
					minInBuffer -= nb;
				}
				currentRead = null;
			}
			canRead.unblock();
		})), true);
	}
	
	private void readBufferForward() {
		int start;
		if (posInBuffer < maxInBuffer) {
			// we have maxInBuffer-posInBuffer bytes remaining at the end
			// bytes which can be read are after those bytes
			start = (maxInBuffer - posInBuffer);
			// we move them at the beginning of the buffer
			System.arraycopy(buffer, posInBuffer, buffer, 0, start);
			// new maximum position in the buffer, until the read is done
			maxInBuffer = start;
			// we start back at then beginning of the buffer
			bufferPosInFile += posInBuffer - minInBuffer;
			posInBuffer = 0;
			minInBuffer = 0;
		} else {
			start = 0;
			bufferPosInFile += maxInBuffer - minInBuffer;
			minInBuffer = 0;
			maxInBuffer = 0;
			posInBuffer = 0;
			canRead.reset();
		}
		// read the bytes
		int len = buffer.length - start;
		if (bufferPosInFile + (maxInBuffer - minInBuffer) + len > fileSize)
			len = (int)(fileSize - (bufferPosInFile + (maxInBuffer - minInBuffer)));
		ByteBuffer buf = ByteBuffer.wrap(buffer, start, len);
		currentRead = io.readFullyAsync(bufferPosInFile + (maxInBuffer - minInBuffer), buf);
		currentRead.listenAsync(operation(new Task.Cpu.FromRunnable("New buffer ready", io.getPriority(), () -> {
			synchronized (BufferedReverseIOReading.this) {
				if (!currentRead.isSuccessful()) {
					error = currentRead.getError();
				} else {
					int nb = currentRead.getResult().intValue();
					maxInBuffer += nb;
				}
				currentRead = null;
			}
			canRead.unblock();
		})), true);
	}

}
