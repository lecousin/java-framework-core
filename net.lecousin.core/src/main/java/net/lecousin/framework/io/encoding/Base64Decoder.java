package net.lecousin.framework.io.encoding;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;

/** Decode Base64 data into a Writable. */
public class Base64Decoder {

	/** Constructor. */
	public Base64Decoder(IO.Writable output) {
		this.output = output;
	}
	
	private IO.Writable output;
	private IAsync<IOException> lastWrite = new Async<>(true);
	private byte[] previous = new byte[4];
	private int nbPrev = 0;
	
	/** Start a new Task to decode the given buffer. */
	public IAsync<IOException> decode(ByteBuffer buffer) {
		Async<IOException> result = new Async<>();
		new Task.Cpu<Void, NoException>("Decoding base 64", output.getPriority()) {
			@Override
			public Void run() {
				if (nbPrev > 0) {
					while (nbPrev < 4 && buffer.hasRemaining())
						previous[nbPrev++] = buffer.get();
					if (doDecodeWithPrevious(buffer, result))
						return null;
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
	public IAsync<IOException> decode(CharBuffer buffer) {
		Async<IOException> result = new Async<>();
		new Task.Cpu<Void, NoException>("Decoding base 64", output.getPriority()) {
			@Override
			public Void run() {
				if (nbPrev > 0) {
					while (nbPrev < 4 && buffer.hasRemaining())
						previous[nbPrev++] = (byte)buffer.get();
					if (doDecodeWithPrevious(buffer, result))
						return null;
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
	
	private boolean doDecodeWithPrevious(Buffer buffer, Async<IOException> result) {
		if (nbPrev < 4) {
			result.unblock();
			return true;
		}
		byte[] out = new byte[3];
		try { Base64.decode4BytesBase64(previous, out); }
		catch (IOException e) {
			result.error(e);
			return true;
		}
		nbPrev = 0;
		if (!buffer.hasRemaining()) {
			write(out, result);
			return true;
		}
		write(out, null);
		return false;
	}
	
	/** Decode any pending bytes, then return a synchronization point that will be unblocked once
	 * the last writing operation is done.
	 */
	public IAsync<IOException> flush() {
		if (nbPrev == 0)
			return lastWrite;
		while (nbPrev < 4)
			previous[nbPrev++] = '=';
		try { write(Base64.decode(previous), null); }
		catch (IOException e) {
			return new Async<>(e);
		}
		return lastWrite;
	}
	
	private void write(byte[] buf, Async<IOException> result) {
		IAsync<IOException> last = lastWrite;
		Async<IOException> thisOne = new Async<>();
		lastWrite = thisOne;
		last.onDone(() -> {
			if (last.hasError()) {
				if (result != null) result.error(last.getError());
				return;
			}
			output.writeAsync(ByteBuffer.wrap(buf)).onDone(thisOne);
			if (result != null) result.unblock();
		});
	}
	
}
