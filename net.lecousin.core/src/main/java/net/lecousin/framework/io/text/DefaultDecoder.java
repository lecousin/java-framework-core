package net.lecousin.framework.io.text;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import net.lecousin.framework.mutable.MutableBoolean;

/** Default implementation of Decoder using JDK CharsetDecoder. */
public class DefaultDecoder extends Decoder {

	/** Constructor. */
	public DefaultDecoder(CharsetDecoder decoder) {
		this.decoder = decoder;
	}
	
	protected CharsetDecoder decoder;
	protected boolean flushed = false;
	protected byte[] remainingBytes = null;
	protected int remainingBytesLength = 0;
	protected CharBuffer nextChars = null; // used in case of overflow, but this is usually an error
	
	@Override
	public void transferTo(Decoder newDecoder) {
		super.transferTo(newDecoder);
		if (remainingBytesLength > 0) {
			if (currentBuffer == null || !currentBuffer.hasRemaining())
				currentBuffer = ByteBuffer.wrap(remainingBytes, 0, remainingBytesLength);
			else {
				byte[] b = new byte[remainingBytesLength + currentBuffer.remaining()];
				System.arraycopy(remainingBytes, 0, b, 0, remainingBytesLength);
				currentBuffer.get(b, remainingBytesLength, currentBuffer.remaining());
				currentBuffer = ByteBuffer.wrap(b);
			}
		}
	}
	
	private void flushNextChars(CharBuffer cb) {
		while (nextChars.hasRemaining() && cb.hasRemaining())
			cb.put(nextChars.get());
		if (!nextChars.hasRemaining())
			nextChars = null;
	}
	
	@Override
	public int decode(ByteBuffer b, char[] chars, int pos, int len, MutableBoolean interrupt, int min) {
		if (b == null) {
			CharBuffer cb = CharBuffer.wrap(chars, pos, len);
			if (nextChars != null) {
				flushNextChars(cb);
				if (!cb.hasRemaining())
					return cb.position() - pos;
			}
			if (flushed) return -1;
			ByteBuffer bb = remainingBytesLength > 0 ? ByteBuffer.wrap(remainingBytes, 0, remainingBytesLength) : ByteBuffer.allocate(0);
			CoderResult cr = decoder.decode(bb, cb, true);
			if (cr.isOverflow()) {
				nextChars = CharBuffer.allocate(10);
				decoder.decode(bb, nextChars, true);
				nextChars.flip();
				flushNextChars(cb);
			}
			remainingBytesLength = 0;
			while (bb.hasRemaining())
				remainingBytes[remainingBytesLength++] = bb.get();
			if (!cb.hasRemaining())
				return len;
			cr = decoder.flush(cb);
			if (cr.isOverflow()) {
				if (nextChars != null) nextChars = CharBuffer.allocate(10);
				else nextChars.compact();
				decoder.flush(nextChars);
				nextChars.flip();
				flushNextChars(cb);
			}
			flushed = true;
			if (cb.position() == pos)
				return -1;
			return cb.position() - pos;
		}
		
		// TODO interrupt/min
		CharBuffer cb = CharBuffer.wrap(chars, pos, len);
		if (nextChars != null) {
			flushNextChars(cb);
			if (!cb.hasRemaining())
				return cb.position() - pos;
		}
		while (remainingBytesLength > 0) {
			while (b.hasRemaining()) {
				remainingBytes[remainingBytesLength++] = b.get();
				ByteBuffer bb = ByteBuffer.wrap(remainingBytes, 0, remainingBytesLength);
				CoderResult cr = decoder.decode(bb, cb, false);
				if (cr.isOverflow()) {
					nextChars = CharBuffer.allocate(10);
					decoder.decode(bb, nextChars, false);
					nextChars.flip();
					flushNextChars(cb);
				}
				if (bb.position() > 0) {
					remainingBytesLength = 0;
					while (bb.hasRemaining())
						remainingBytes[remainingBytesLength++] = bb.get();
					break;
				}
			}
			if (!cb.hasRemaining())
				return len;
			if (!b.hasRemaining())
				return cb.position() - pos;
		}
		if (!b.hasRemaining())
			return cb.position() - pos;
		
		CoderResult cr = decoder.decode(b, cb, false);
		if (cr.isUnderflow() && b.hasRemaining()) {
			if (remainingBytes == null) remainingBytes = new byte[16];
			while (b.hasRemaining())
				remainingBytes[remainingBytesLength++] = b.get();
		} else if (cr.isOverflow()) {
			nextChars = CharBuffer.allocate(10);
			decoder.decode(b, nextChars, false);
			nextChars.flip();
			flushNextChars(cb);
		}
		return cb.position() - pos;
	}
	
	@Override
	public Charset getEncoding() {
		return decoder.charset();
	}
	
}
