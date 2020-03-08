package net.lecousin.framework.encoding;

import java.util.BitSet;

import net.lecousin.framework.encoding.charset.UTF8Decoder;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.memory.CharArrayCache;
import net.lecousin.framework.text.CharArrayStringBuffer;

/**
 * Encoding of URL.<br/>
 * This class is similar to the {@link java.net.URLEncoder} and {@link java.net.URLDecoder}, but
 * those 2 classes provided in the JDK are accepting to encode using any charset, even it is stated in
 * the comments that only UTF-8 should be used. This implies a more generic but less efficient implementation.<br/>
 * This class implements an efficient way using only UTF-8 charset.
 */
public final class URLEncoding {

	private URLEncoding() {
		// no instance
	}
	
	private static BitSet dontNeedEncoding;
	
	static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');

	}

	/**
	 * Encode the given characters into the given bytes.
	 * <p>
	 * This method consumes as much as characters as possible to encode, and can be
	 * called several time to consume all characters and produce bytes step by step.
	 * </p>
	 * <p>
	 * The parameter isPath indicates if we are encoding a path. In this case, the
	 * slash (/) character is not encoded.
	 * </p>
	 * <p>
	 * The parameter endOfChars indicates if no more characters will come, to avoid
	 * a blocking situation where 2 characters are expected to continue, but only one
	 * is remaining. In this case the final character is consumed without producing any
	 * byte as this means it cannot be encoded correctly into UTF-8.
	 * </p>
	 * 
	 * @param chars the characters to consume
	 * @param out the bytes to produce
	 * @param isPath if true, slash (/) character is not encoded
	 * @param endOfChars true if there will be no more characters coming
	 */
	@SuppressWarnings("java:S3776") // complexity
	public static void encode(Chars.Readable chars, Bytes.Writable out, boolean isPath, boolean endOfChars) {
		while (out.hasRemaining() && chars.hasRemaining()) {
			char c = chars.get();
			if (c < 0x80) {
				if (c == ' ') {
					out.put((byte)'+');
				} else if (dontNeedEncoding.get(c) || (c == '/' && isPath)) {
					out.put((byte)c);
				} else {
					// 0aaa aaaa => 1 byte encoded
					if (out.remaining() < 3) {
						chars.setPosition(chars.position() - 1);
						break;
					}
					out.put((byte)'%');
					out.put((byte)HexaDecimalEncoding.encodeDigit((c & 0xF0) >> 4));
					out.put((byte)HexaDecimalEncoding.encodeDigit(c & 0x0F));
				}
			} else if (c < 0x800) {
				// 110a aaaa 10bb bbbb => 2 bytes encoded
				if (out.remaining() < 6) {
					chars.setPosition(chars.position() - 1);
					break;
				}
				out.put((byte)'%');
				out.put((byte)HexaDecimalEncoding.encodeDigit(0xC | ((c & 0x400) >> 10)));
				out.put((byte)HexaDecimalEncoding.encodeDigit((c & 0x3C0) >> 6));
				out.put((byte)'%');
				out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | ((c & 0x30) >> 4)));
				out.put((byte)HexaDecimalEncoding.encodeDigit(c & 0xF));
			} else if (c >= 0xD800 && c <= 0xDBFF) {
				// start of a surrogate pair, we need 2 characters that will be encoded into 4 bytes
				if (!chars.hasRemaining()) {
					if (!endOfChars)
						chars.setPosition(chars.position() - 1);
					break;
				}
				if (out.remaining() < 12) {
					chars.setPosition(chars.position() - 1);
					break;
				}
				int c2 = chars.get();
				if (c2 >= 0xDC00 && c2 <= 0xDFFF) {
					// UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
					// Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
					//          [1101 11yy] [yyxx xxxx] (low surrogate)
					//          * uuuuu = wwww + 1
					int uuuuu = ((c & 0x03C0) >> 6) + 1;
					out.put((byte)'%');
					out.put((byte)HexaDecimalEncoding.encodeDigit(0xF));
					out.put((byte)HexaDecimalEncoding.encodeDigit((uuuuu & 0x1C) >> 2));
					out.put((byte)'%');
					out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | (uuuuu & 0x3)));
					out.put((byte)HexaDecimalEncoding.encodeDigit((c & 0x3C) >> 2));
					out.put((byte)'%');
					out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | (c & 0x3)));
					out.put((byte)HexaDecimalEncoding.encodeDigit((c2 & 0x3C0) >> 6));
					out.put((byte)'%');
					out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | ((c2 & 0x30) >> 4)));
					out.put((byte)HexaDecimalEncoding.encodeDigit(c2 & 0xF));
				}
			} else {
				// 1110 aaaa 10bb bbbb 10cc cccc
				if (out.remaining() < 9) {
					chars.setPosition(chars.position() - 1);
					break;
				}
				out.put((byte)'%');
				out.put((byte)HexaDecimalEncoding.encodeDigit(0xE));
				out.put((byte)HexaDecimalEncoding.encodeDigit((c & 0xF000) >> 12));
				out.put((byte)'%');
				out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | (c & 0xC00) >> 10));
				out.put((byte)HexaDecimalEncoding.encodeDigit((c & 0x3C0) >> 6));
				out.put((byte)'%');
				out.put((byte)HexaDecimalEncoding.encodeDigit(0x8 | (c & 0x30) >> 4));
				out.put((byte)HexaDecimalEncoding.encodeDigit(c & 0xF));
			}
		}
	}
	
	/** Decode encoded URL bytes into decoded characters.
	 * 
	 * @param bytes encoded URL
	 * @param out decoded URL
	 * @param endOfBytes true if there will be no more bytes coming
	 */
	@SuppressWarnings("java:S3776") // complexity
	public static void decode(Bytes.Readable bytes, Chars.Writable out, boolean endOfBytes) {
		while (bytes.hasRemaining() && out.hasRemaining()) {
			byte b = bytes.get();
			if (b == '+') {
				out.put(' ');
			} else if (b == '%') {
				if (bytes.remaining() < 2) {
					if (endOfBytes)
						bytes.goToEnd();
					else
						bytes.setPosition(bytes.position() - 1);
					break;
				}
				int v1;
				try { v1 = (HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)) << 4); }
				catch (EncodingException e) {
					out.put('%');
					bytes.setPosition(bytes.position() - 1);
					continue;
				}
				try { v1 |= HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)); }
				catch (EncodingException e) {
					out.put('%');
					bytes.setPosition(bytes.position() - 2);
					continue;
				}
				
				if ((v1 & 0x80) == 0) {
					// 0aaa aaaa
					out.put((char)v1);
					continue;
				}
				
				if (bytes.remaining() < 3) {
					if (endOfBytes) {
						out.put((char)v1);
						continue;
					}
					bytes.setPosition(bytes.position() - 3);
					break;
				}
				b = bytes.get();
				if (b != '%') {
					out.put((char)v1);
					bytes.setPosition(bytes.position() - 1);
					continue;
				}

				int v2;
				try { v2 = (HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)) << 4); }
				catch (EncodingException e) {
					out.put((char)v1);
					bytes.setPosition(bytes.position() - 2);
					continue;
				}
				try { v2 |= HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)); }
				catch (EncodingException e) {
					out.put((char)v1);
					bytes.setPosition(bytes.position() - 3);
					continue;
				}

				
				if ((v1 & 0xE0) == 0xC0) {
					// 110a aaaa 10bb bbbb
					if ((v2 & 0xC0) == 0x80)
						out.put((char)(((v1 & 0x1F) << 6) | (v2 & 0x3F)));
					else
						out.put(UTF8Decoder.INVALID_CHAR);
					continue;
				}
				
				if (bytes.remaining() < 3) {
					if (endOfBytes)
						continue;
					bytes.setPosition(bytes.position() - 6);
					break;
				}
				b = bytes.get();
				if (b != '%') {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 1);
					continue;
				}
				
				int v3;
				try { v3 = (HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)) << 4); }
				catch (EncodingException e) {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 2);
					continue;
				}
				try { v3 |= HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)); }
				catch (EncodingException e) {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 3);
					continue;
				}
				
				if ((v1 & 0xF0) == 0xE0) {
					// 1110 aaaa 10bb bbbb 10cc cccc
					if ((v2 & 0xC0) == 0x80 && (v3 & 0xC0) == 0x80)
						out.put((char)(((v1 & 0x0F) << 12) | ((v2 & 0x3F) << 6) | (v3 & 0x3F)));
					else
						out.put(UTF8Decoder.INVALID_CHAR);
					continue;
				}
				
				if (bytes.remaining() < 3) {
					if (endOfBytes)
						continue;
					bytes.setPosition(bytes.position() - 9);
					break;
				}
				b = bytes.get();
				if (b != '%') {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 1);
					continue;
				}
				
				int v4;
				try { v4 = (HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)) << 4); }
				catch (EncodingException e) {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 2);
					continue;
				}
				try { v4 |= HexaDecimalEncoding.decodeChar((char)(bytes.get() & 0xFF)); }
				catch (EncodingException e) {
					out.put(UTF8Decoder.INVALID_CHAR);
					bytes.setPosition(bytes.position() - 3);
					continue;
				}

				if ((v1 & 0xF8) == 0xF0) {
					// 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
					if ((v2 & 0xC0) == 0x80 && (v3 & 0xC0) == 0x80 && (v4 & 0xC0) == 0x80) {
						if (out.remaining() < 2) {
							bytes.setPosition(bytes.position() - 12);
							break;
						}
						// UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
						// Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
						//          [1101 11yy] [yyxx xxxx] (low surrogate)
						//          * uuuuu = wwww + 1
						int v = ((v1 & 0x07) << 12) | ((v2 & 0x3F) << 6) | (v3 & 0x3F);
						int uuuuu = (v & 0x7C00) >> 10;
						if (uuuuu > 0x10) {
							// invalid surrogate
							out.put(UTF8Decoder.INVALID_CHAR);
							continue;
						}
						int wwww = uuuuu - 1;
						out.put((char)(0xD800 | (wwww << 6) | ((v & 0x3C0) >> 4) | ((v & 0x30) >> 4)));
						out.put((char)(0xDC00 | ((v & 0xF) << 6) | (v4 & 0x3F)));
					} else {
						out.put(UTF8Decoder.INVALID_CHAR);
					}
				} else {
					out.put(UTF8Decoder.INVALID_CHAR);
				}
			} else {
				out.put((char)(b & 0xFF));
			}
		}
	}
	
	/** Convenient method to decode the given bytes into a string. */
	public static CharArrayStringBuffer decode(Bytes.Readable bytes) {
		CharArrayStringBuffer chars = new CharArrayStringBuffer();
		CharArrayCache cache = CharArrayCache.getInstance();
		do {
			char[] buf = cache.get(Math.max(bytes.remaining(), 128), true);
			CharArray.Writable ca = new CharArray.Writable(buf, true);
			decode(bytes, ca, true);
			if (ca.position() > 0) {
				chars.append(buf, 0, ca.position());
			} else {
				cache.free(buf);
			}
		} while (bytes.hasRemaining());
		return chars;
	}
	
}
