package net.lecousin.framework.io.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;

/** Decode Base64 data into a Writable. */
public class Base64Decoder {

	/** Constructor. */
	public Base64Decoder(IO.Writable output) {
		this.output = output;
	}
	
	private IO.Writable output;
	private ISynchronizationPoint<IOException> lastWrite = new SynchronizationPoint<>(true);
	private byte[] previous = new byte[4];
	private int nbPrev = 0;
	
	/** Start a new Task to decode the given buffer. */
	public ISynchronizationPoint<IOException> decode(ByteBuffer buffer) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		new Task.Cpu<Void, NoException>("Decoding base 64", output.getPriority()) {
			@Override
			public Void run() {
				if (nbPrev > 0) {
					while (nbPrev < 4 && buffer.hasRemaining())
						previous[nbPrev++] = buffer.get();
					if (nbPrev < 4) {
						result.unblock();
						return null;
					}
					byte[] out = new byte[3];
					try { Base64.decode4BytesBase64(previous, out); }
					catch (IOException e) {
						result.error(e);
						return null;
					}
					nbPrev = 0;
					if (!buffer.hasRemaining()) {
						write(out, result);
						return null;
					}
					write(out, null);
				}
				byte[] out;
				try { out = Base64.decode(buffer); }
				catch (IOException e) {
					result.error(e);
					return null;
				}
				while (buffer.hasRemaining())
					previous[nbPrev++] = buffer.get();
				write(out, result);
				return null;
			}
		}.start();
		return result;
	}
	
	/** Start a new Task to decode the given buffer. */
	public ISynchronizationPoint<IOException> decode(CharBuffer buffer) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		new Task.Cpu<Void, NoException>("Decoding base 64", output.getPriority()) {
			@Override
			public Void run() {
				if (nbPrev > 0) {
					while (nbPrev < 4 && buffer.hasRemaining())
						previous[nbPrev++] = (byte)buffer.get();
					if (nbPrev < 4) {
						result.unblock();
						return null;
					}
					byte[] out = new byte[3];
					try { Base64.decode4BytesBase64(previous, out); }
					catch (IOException e) {
						result.error(e);
						return null;
					}
					nbPrev = 0;
					if (!buffer.hasRemaining()) {
						write(out, result);
						return null;
					}
					write(out, null);
				}
				byte[] out;
				try { out = Base64.decode(buffer); }
				catch (IOException e) {
					result.error(e);
					return null;
				}
				while (buffer.hasRemaining())
					previous[nbPrev++] = (byte)buffer.get();
				write(out, result);
				return null;
			}
		}.start();
		return result;
	}
	
	/** Decode any pending bytes, then return a synchronization point that will be unblocked once
	 * the last writing operation is done.
	 */
	public ISynchronizationPoint<IOException> flush() {
		if (nbPrev == 0)
			return lastWrite;
		while (nbPrev < 4)
			previous[nbPrev++] = '=';
		try { write(Base64.decode(previous), null); }
		catch (IOException e) {
			return new SynchronizationPoint<>(e);
		}
		return lastWrite;
	}
	
	private void write(byte[] buf, SynchronizationPoint<IOException> result) {
		ISynchronizationPoint<IOException> last = lastWrite;
		SynchronizationPoint<IOException> thisOne = new SynchronizationPoint<>();
		lastWrite = thisOne;
		last.listenInline(() -> {
			if (last.hasError()) {
				if (result != null) result.error(last.getError());
				return;
			}
			output.writeAsync(ByteBuffer.wrap(buf)).listenInline(thisOne);
			if (result != null) result.unblock();
		});
	}
	
}
