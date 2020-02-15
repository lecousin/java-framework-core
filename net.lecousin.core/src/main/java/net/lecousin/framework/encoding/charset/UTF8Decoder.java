package net.lecousin.framework.encoding.charset;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CharsFromIso8859Bytes;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.memory.CharArrayCache;

/** UTF-8 Decoder. */
public class UTF8Decoder implements CharacterDecoder {

	/** Constructor. */
	public UTF8Decoder(int bufferSize) {
		this.bufferSize = Math.max(2, bufferSize);
		cache = CharArrayCache.getInstance();
		charPusher = new InitCharPusher();
	}
	
	private CharArrayCache cache;
	private int bufferSize;
	private CharArray.Writable buffer;
	private CharPusher charPusher;
	private int val = 0;
	private int state = 0;
	CompositeChars.Readable result;
	
	public static final char INVALID_CHAR = '�';

	@Override
	public Charset getEncoding() {
		return StandardCharsets.UTF_8;
	}
	
	@Override
	@SuppressWarnings({
		"squid:S3776" // do not reduce complexity that may reduce performance
	})
	public Chars.Readable decode(Bytes.Readable input) {
		result = new CompositeChars.Readable();
		
		// if previous buffer was stopped in a specific state, we need to finish previous decoding
		if (state != 0) {
			finishState(input);
			if (!input.hasRemaining()) {
				input.free();
				return result();
			}
		}
		
		// while there are at least 4 remaining bytes we are sure there is enough input to decode a full character
		// so less conditions to check
		boolean inputReused = false;
		while (input.remaining() >= 4) {
			byte b = input.get();
			if (b >= 0) {
				// 0aaa aaaa
				// => ascii character, let's check if there are enough to use a BytesAsAsciiChars
				if (input.getForward(0) < 0) {
					// only one
					charPusher.push((char)b);
					continue;
				}
				int r = input.remaining();
				int endOfAscii = 1;
				while (endOfAscii < r && input.getForward(endOfAscii) >= 0)
					endOfAscii++;
				if (endOfAscii >= 4 || endOfAscii == r) {
					pushBuffer();
					result.add(new CharsFromIso8859Bytes(input.subBuffer(input.position() - 1, endOfAscii + 1), false));
					input.moveForward(endOfAscii);
					inputReused = true;
					continue;
				}
				charPusher.push((char)b);
				for (int i = 0; i < endOfAscii; ++i)
					charPusher.push((char)input.get());
				continue;
			}
			
			if ((b & 0xE0) == 0xC0) {
				// 110a aaaa 10bb bbbb
				byte b2 = input.get();
				if ((b2 & 0xC0) == 0x80)
					charPusher.push((char)(((b & 0x1F) << 6) | (b2 & 0x3F)));
				else
					charPusher.push(INVALID_CHAR);
			} else if ((b & 0xF0) == 0xE0) {
				// 1110 aaaa 10bb bbbb 10cc cccc
				byte b2 = input.get();
				byte b3 = input.get();
				if ((b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80)
					charPusher.push((char)(((b & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
				else
					charPusher.push(INVALID_CHAR);
			} else if ((b & 0xF8) == 0xF0) {
				// 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
				byte b2 = input.get();
				byte b3 = input.get();
				byte b4 = input.get();
				if ((b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80 && (b4 & 0xC0) == 0x80) {
					// UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
					// Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
					//          [1101 11yy] [yyxx xxxx] (low surrogate)
					//          * uuuuu = wwww + 1
					int v = ((b & 0x07) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
					int uuuuu = (v & 0x7C00) >> 10;
					if (uuuuu > 0x10) {
						// invalid surrogate
						charPusher.push(INVALID_CHAR);
						continue;
					}
					int wwww = uuuuu - 1;
					charPusher.push((char)(0xD800 | (wwww << 6) | ((v & 0x3C0) >> 4) | ((v & 0x30) >> 4)));
					charPusher.push((char)(0xDC00 | ((v & 0xF) << 6) | (b4 & 0x3F)));
				} else {
					charPusher.push(INVALID_CHAR);
				}
			} else {
				charPusher.push(INVALID_CHAR);
			}
		}
		
		while (input.hasRemaining()) {
			if (state == 0) {
				byte b = input.get();
				if (b >= 0) {
					charPusher.push((char)b);
				} else {
					if ((b & 0xE0) == 0xC0) {
						// 110a aaaa 10bb bbbb
						val = (b & 0x1F) << 6;
						state = 1;
					} else if ((b & 0xF0) == 0xE0) {
						// 1110 aaaa 10bb bbbb 10cc cccc
						val = (b & 0x0F) << 12;
						state = 2;
					} else if ((b & 0xF8) == 0xF0) {
						// 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
						val = (b & 0x07) << 12;
						state = 4;
					} else {
						charPusher.push(INVALID_CHAR);
					}
				}
			} else {
				finishState(input);
			}
		}
		
		if (!inputReused)
			input.free();
		else
			input.goToEnd();
		return result();
	}
	
	private interface CharPusher {
		void push(char c);
	}
	
	private class InitCharPusher implements CharPusher {
		@Override
		public void push(char c) {
			buffer = new CharArray.Writable(cache.get(bufferSize, true), true);
			buffer.put(c);
			charPusher = new BufferCharPusher();
		}
	}
	
	private class BufferCharPusher implements CharPusher {
		@Override
		public void push(char c) {
			buffer.put(c);
			if (!buffer.hasRemaining()) {
				buffer.flip();
				result.add(buffer);
				buffer = null;
				charPusher = new InitCharPusher();
			}
		}
	}
	
	private void pushBuffer() {
		if (buffer == null)
			return;
		int l = buffer.position();
		if (l > 0) {
			result.add(buffer.subBuffer(0, l));
			buffer.slice();
			charPusher = new BufferCharPusher();
		}
	}
	
	private Chars.Readable result() {
		pushBuffer();
		if (result.getWrappedBuffers().size() == 1)
			return result.getWrappedBuffers().get(0);
		return result;
	}
	
	private void finishState(Bytes.Readable input) {
		switch (state) {
		case 1: case 3:
			finishState1or3(input);
			return;
		case 2:
			finishState2(input);
			return;
		case 4:
			finishState4(input);
			return;
		case 5:
			finishState5(input);
			return;
		default: //case 6:
			finishState6(input);
			return;
		}
	}

	private void finishState1or3(Bytes.Readable input) {
		// 1: second byte of 110a aaaa 10bb bbbb
		// 3: third byte of 1110 aaaa 10bb bbbb 10cc cccc
		byte b = input.get();
		if ((b & 0xC0) == 0x80) {
			charPusher.push((char)(val | (b & 0x3F)));
		} else {
			charPusher.push(INVALID_CHAR);
		}
		state = 0;
	}
	
	private void finishState2(Bytes.Readable input) {
		// second byte of 1110 aaaa 10bb bbbb 10cc cccc
		byte b = input.get();
		if ((b & 0xC0) == 0x80) {
			val |= (b & 0x3F) << 6;
			if (input.hasRemaining())
				finishState1or3(input);
			else
				state = 3;
		} else {
			charPusher.push(INVALID_CHAR);
			state = 0;
		}
	}
	
	private void finishState4(Bytes.Readable input) {
		// second byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
		byte b = input.get();
		if ((b & 0xC0) == 0x80) {
			val |= (b & 0x3F) << 6;
			if (input.hasRemaining())
				finishState5(input);
			else
				state = 5;
		} else {
			charPusher.push(INVALID_CHAR);
			state = 0;
		}
	}
	
	private void finishState5(Bytes.Readable input) {
		// third byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
		byte b = input.get();
		if ((b & 0xC0) == 0x80) {
			val |= (b & 0x3F);
			if (input.hasRemaining())
				finishState6(input);
			else
				state = 6;
		} else {
			charPusher.push(INVALID_CHAR);
			state = 0;
		}
	}
	
	private void finishState6(Bytes.Readable input) {
		// fourth byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
		byte b = input.get();
		if ((b & 0xC0) == 0x80) {
			// UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
			// Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
			//          [1101 11yy] [yyxx xxxx] (low surrogate)
			//          * uuuuu = wwww + 1
			int uuuuu = (val & 0x7C00) >> 10;
			if (uuuuu > 0x10) {
				// invalid surrogate
				charPusher.push(INVALID_CHAR);
			} else {
				int wwww = uuuuu - 1;
				charPusher.push((char)(0xD800 | (wwww << 6) | ((val & 0x3C0) >> 4) | ((val & 0x30) >> 4)));
				charPusher.push((char)(0xDC00 | ((val & 0xF) << 6) | (b & 0x3F)));
			}
		} else {
			charPusher.push(INVALID_CHAR);
		}
		state = 0;
	}
	
	@Override
	public Chars.Readable flush() {
		result = null;
		charPusher = new InitCharPusher();
		buffer = null;
		if (state == 0)
			return null;
		state = 0;
		return new CharArray(new char[] { (char)val });
	}
	
}
