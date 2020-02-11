package net.lecousin.framework.encoding.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.memory.CharArrayCache;

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
		CharBuffer out = CharBuffer.wrap(CharArrayCache.getInstance().get(bufferSize, true));
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
					input.free();
					if (out.position() > 0)
						result.add(new CharArray.Writable(out.array(), 0, out.position(), true));
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
				result.add(new CharArray.Writable(out.array(), out.arrayOffset(), out.position(), true));
			if (!res.isOverflow())
				break;
			out = CharBuffer.wrap(CharArrayCache.getInstance().get(bufferSize, true));
		} while (true);
		if (in.hasRemaining()) {
			// keep remaining bytes
			remainingBytes = ByteArrayCache.getInstance().get(32, true);
			nbRemaining = in.remaining();
			in.get(remainingBytes, 0, nbRemaining);
		}
		input.goToEnd();
		input.free();
		return result.getWrappedBuffers().size() == 1 ? result.getWrappedBuffers().get(0) : result;
	}

	@Override
	public Chars.Readable flush() {
		if (remainingBytes == null) {
			decoder.reset();
			return null;
		}
		CharBuffer out = CharBuffer.allocate(8);
		decoder.decode(ByteBuffer.wrap(remainingBytes, 0, nbRemaining), out, true);
		decoder.reset();
		remainingBytes = null;
		nbRemaining = 0;
		if (out.position() == 0)
			return null;
		return new CharArray(out.array(), 0, out.position());
	}
	
	@Override
	public Charset getEncoding() {
		return decoder.charset();
	}
}
