package net.lecousin.framework.io.bit;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.io.IO;

/**
 * Simple implementation of a Readable BitIO, based on a Buffered Readable IO.
 * It reads byte after byte on demand from the IO, meaning no additional byte is read in advance or buffered.
 * This means it can be used in a middle of "normal" read operations on the IO.
 */
public abstract class SimpleReadableBitIO extends AbstractBitIO<IO.Readable.Buffered> implements BitIO.Readable {

	protected SimpleReadableBitIO(IO.Readable.Buffered io) {
		super(io);
	}

	protected long currentValue;
	protected int currentNbBits = 0;
	protected boolean eof = false;
	
	@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return io.canStartReading();
	}
	
	@Override
	public boolean readBoolean() throws IOException {
		if (currentNbBits == 0) {
			currentValue = io.read();
			if (currentValue == -1) {
				eof = true;
				throw new EOFException();
			}
			currentNbBits = 7;
			return getBooleanValueFromByte();
		}
		return getNextBooleanValue();
	}

	@Override
	public Boolean readBooleanIfAvailable() throws IOException {
		if (currentNbBits == 0) {
			currentValue = io.readAsync();
			if (currentValue == -1) {
				eof = true;
				throw new EOFException();
			}
			if (currentValue == -2) return null;
			currentNbBits = 7;
			return Boolean.valueOf(getBooleanValueFromByte());
		}
		return Boolean.valueOf(getNextBooleanValue());
	}
	
	protected abstract boolean getBooleanValueFromByte();
	
	protected abstract boolean getNextBooleanValue();
	
	protected abstract long getRemainingBits();
	
	@Override
	public long readBits(int n) throws IOException {
		do {
			if (n < currentNbBits)
				return getNextBits(n);
			if (n == currentNbBits) {
				long val = getRemainingBits();
				currentValue = 0;
				currentNbBits = 0;
				return val;
			}
			if (eof) throw new EOFException();
			int b = io.read();
			if (b == -1) {
				eof = true;
				throw new EOFException();
			}
			pushNewByte(b);
			currentNbBits += 8;
		} while (true);
	}
	
	@Override
	public long readBitsIfAvailable(int n) throws IOException {
		do {
			if (n < currentNbBits)
				return getNextBits(n);
			if (n == currentNbBits) {
				long val = getRemainingBits();
				currentNbBits = 0;
				currentValue = 0;
				return val;
			}
			if (eof) throw new EOFException();
			int b = io.readAsync();
			if (b == -2) return -1;
			if (b == -1) {
				eof = true;
				throw new EOFException();
			}
			pushNewByte(b);
			currentNbBits += 8;
		} while (true);
	}
	
	protected abstract long getNextBits(int n);
	
	protected abstract void pushNewByte(int b);

	/** Simple implementation of a Readable BitIO in big endian order, based on a Buffered Readable IO. */
	public static class BigEndian extends SimpleReadableBitIO implements BitIO.BigEndian {
		
		/** Constructor. */
		public BigEndian(IO.Readable.Buffered io) {
			super(io);
		}
		
		@Override
		protected boolean getBooleanValueFromByte() {
			return (currentValue & 0x80) != 0;
		}
		
		@Override
		protected boolean getNextBooleanValue() {
			return (currentValue & (1 << (--currentNbBits))) != 0;
		}
	
		@Override
		protected long getNextBits(int n) {
			long value = (currentValue >> (currentNbBits - n)) & ((1 << n) - 1);
			currentNbBits -= n;
			return value;
		}
		
		@Override
		protected long getRemainingBits() {
			return currentValue & ((1 << currentNbBits) - 1);
		}
		
		@Override
		protected void pushNewByte(int b) {
			currentValue = (currentValue << 8) | b;
		}
	}

	/** Simple implementation of a Readable BitIO in big endian order, based on a Buffered Readable IO. */
	public static class LittleEndian extends SimpleReadableBitIO implements BitIO.BigEndian {
		
		/** Constructor. */
		public LittleEndian(IO.Readable.Buffered io) {
			super(io);
		}
		
		@Override
		protected boolean getBooleanValueFromByte() {
			boolean value = (currentValue & 0x01) != 0;
			currentValue >>= 1;
			return value;
		}
		
		@Override
		protected boolean getNextBooleanValue() {
			boolean value = (currentValue & 0x01) != 0;
			currentValue >>= 1;
			currentNbBits--;
			return value;
		}

		@Override
		protected long getNextBits(int n) {
			long value = (currentValue & ((1 << n) - 1));
			currentValue >>= n;
			currentNbBits -= n;
			return value;
		}
		
		@Override
		protected long getRemainingBits() {
			return currentValue;
		}
		
		@Override
		protected void pushNewByte(int b) {
			currentValue |= (b << currentNbBits);
		}

	}
	
}
