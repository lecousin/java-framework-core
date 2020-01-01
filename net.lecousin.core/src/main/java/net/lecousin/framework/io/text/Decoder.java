package net.lecousin.framework.io.text;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.AsyncCloseable;

/** Charset decoder. */
public abstract class Decoder implements Closeable, AsyncCloseable<IOException> {
	
	/** Get a decoder for the given charset. */
	public static Decoder get(Charset charset) throws InstantiationException, IllegalAccessException {
		Class<? extends Decoder> cl = decoders.get(charset.name());
		if (cl == null) {
			CharsetDecoder decoder = charset.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPLACE);
			decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
			return new DefaultDecoder(decoder);
		}
		return cl.newInstance();
	}
	
	/** Register a decoder implementation. */
	public static void register(Charset charset, Class<? extends Decoder> decoder) {
		decoders.put(charset.name(), decoder);
	}
	
	private static Map<String, Class<? extends Decoder>> decoders = new HashMap<>();
	
	static {
		register(StandardCharsets.UTF_8, UTF8Decoder.class);
	}
	
	/** Set first bytes to decode before the IO. */
	public void setFirstBytes(byte[] bytes) {
		setFirstBytes(ByteBuffer.wrap(bytes));
	}
	
	/** Set first bytes to decode before the IO. */
	public void setFirstBytes(ByteBuffer bytes) {
		currentBuffer = bytes;
	}

	/** Set readable IO to decode. */
	public void setInput(IO.Readable.Buffered io) {
		this.io = io;
		this.nextBuffer = io.readNextBufferAsync();
	}
	
	/** Change charset by transfering the current state of this decoder to the new one. */
	public void transferTo(Decoder newDecoder) {
		newDecoder.io = io;
		newDecoder.nextBuffer = nextBuffer;
		newDecoder.currentBuffer = currentBuffer;
	}
	
	protected IO.Readable.Buffered io;
	protected AsyncSupplier<ByteBuffer, IOException> nextBuffer;
	protected ByteBuffer currentBuffer = null;
	
	/** When the decode method returns -2, this method can be used to know when new data is available to be decoded. */
	public IAsync<IOException> canDecode() {
		if (currentBuffer != null) return new Async<>(true);
		return nextBuffer;
	}
	
	/**
	 * Decode characters from the IO.
	 * @param chars array to fill
	 * @param pos offset in the array to fill
	 * @param len maximum number of characters to fill
	 * @param interrupt when true, the decoding will stop as soon as possible
	 * @param min minimum number of characters to fill, regardless of the interrupt value
	 * @return number of decoded characters, -1 if no more characters can be decoded, -2 if we need to wait for next buffer from the IO
	 * @throws IOException error reading from IO
	 * @throws CancelException IO has been cancelled
	 */
	public int decode(char[] chars, int pos, int len, MutableBoolean interrupt, int min) throws IOException, CancelException {
		if (min > len) min = len;
		int nb = 0;
		do {
			if (currentBuffer == null) {
				if (!nextBuffer.isDone())
					return nb > 0 ? nb : -2;
				currentBuffer = nextBuffer.blockResult(0);
				if (currentBuffer != null)
					nextBuffer = io.readNextBufferAsync();
			}
			int decoded = decode(currentBuffer, chars, pos + nb, len - nb, interrupt, min - nb);
			if (decoded < 0 || (decoded == 0 && currentBuffer == null))
				return nb > 0 ? nb : -1;
			nb += decoded;
			if (currentBuffer != null && !currentBuffer.hasRemaining())
				currentBuffer = null;
			if (nb == len || (nb >= min && interrupt.get()))
				return nb;
		} while (true);
	}

	/** Decode characters without IO. The full buffer is read.
	 * If b is null, the remaining bytes from the previous operation are decoded. */
	public int decode(ByteBuffer b, char[] chars, int pos, int len) {
		if (io != null)
			throw new IllegalStateException();
		return decode(b, chars, pos, len, null, 1);
	}
	
	/** Return this number of decoded characters, or -1 if no more character can be decoded. */
	protected abstract int decode(ByteBuffer b, char[] chars, int pos, int len, MutableBoolean interrupt, int min);
	
	/** Return the charset of this decoder. */
	public abstract Charset getEncoding();
	
	@Override
	public void close() throws IOException {
		try { io.close(); }
		catch (IOException e) { throw e; }
		catch (Exception e) { throw IO.error(e); }
	}
	
	@Override
	public IAsync<IOException> closeAsync() {
		return io.closeAsync();
	}
	
}
