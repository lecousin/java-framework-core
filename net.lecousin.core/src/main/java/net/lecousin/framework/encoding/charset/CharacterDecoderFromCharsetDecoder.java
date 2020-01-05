package net.lecousin.framework.encoding.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.RawCharBuffer;
import net.lecousin.framework.memory.ByteArrayCache;

/** Default decoder using a standard CharsetDecoder. */
public class CharacterDecoderFromCharsetDecoder implements CharacterDecoder {

	/** Constructor. */
	public CharacterDecoderFromCharsetDecoder(CharsetDecoder decoder, int bufferSize) {
		this.decoder = decoder;
		this.bufferSize = bufferSize;
	}
	
	private CharsetDecoder decoder;
	private int bufferSize;
	private byte[] remainingBytes;
	private int nbRemaining;

	@Override
	public Chars.Readable decode(Bytes.Readable input) {
		CompositeChars.Readable result = new CompositeChars.Readable();
		CharBuffer out = CharBuffer.allocate(bufferSize);
		if (remainingBytes != null) {
			do {
				remainingBytes[nbRemaining++] = input.get();
				ByteBuffer in = ByteBuffer.wrap(remainingBytes, 0, nbRemaining);
				CoderResult res = decoder.decode(in, out, false);
				if (!res.isUnderflow())
					break;
				if (in.position() > 0) {
					nbRemaining = in.remaining();
					System.arraycopy(remainingBytes, in.position(), remainingBytes, 0, nbRemaining);
				}
				if (!input.hasRemaining()) {
					if (out.position() > 0)
						result.add(new RawCharBuffer(out.array(), 0, out.position()));
					return result;
				}
			} while (true);
			ByteArrayCache.getInstance().free(remainingBytes);
			remainingBytes = null;
		}
		ByteBuffer in = input.toByteBuffer();
		do {
			CoderResult res = decoder.decode(in, out, false);
			if (out.position() > 0)
				result.add(new RawCharBuffer(out.array(), out.arrayOffset(), out.position()));
			if (!res.isOverflow())
				break;
			out = CharBuffer.allocate(bufferSize);
		} while (true);
		if (in.hasRemaining()) {
			// keep remaining bytes
			remainingBytes = ByteArrayCache.getInstance().get(32, true);
			nbRemaining = in.remaining();
			in.get(remainingBytes, 0, nbRemaining);
		}
		input.goToEnd();
		return result.getWrappedChars().size() == 1 ? result.getWrappedChars().get(0) : result;
	}

	@Override
	public Chars.Readable flush() {
		if (remainingBytes == null)
			return null;
		CharBuffer out = CharBuffer.allocate(8);
		decoder.decode(ByteBuffer.wrap(remainingBytes, 0, nbRemaining), out, true);
		if (out.position() == 0)
			return null;
		return new RawCharBuffer(out.array(), 0, out.position());
	}
	
	@Override
	public Charset getEncoding() {
		return decoder.charset();
	}
}
