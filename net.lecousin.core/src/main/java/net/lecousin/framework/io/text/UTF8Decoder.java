package net.lecousin.framework.io.text;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.mutable.MutableBoolean;

/** Decoder for UTF-8 charset. */
public class UTF8Decoder extends Decoder {
	
	private int val = 0;
	private int state = 0;
	private static final char INVALID_CHAR = '�';

	// skip checkstyle: MissingSwitchDefault
	@SuppressWarnings({"squid:SwitchLastCaseIsDefaultCheck", "squid:AssignmentInSubExpressionCheck", "squid:S3776"})
	@Override
	protected int decode(ByteBuffer b, char[] chars, int pos, int len, MutableBoolean interrupt, int min) {
		if (b == null) {
			// end
			if (state == 0)
				return -1;
			chars[pos] = (char)val;
			state = 0;
			return 1;
		}
		byte a;
		byte[] arr = b.array();
		int arrPos = b.arrayOffset() + b.position();
		int arrMax = arrPos + b.remaining();
		int init = pos;
		int end = pos + len;
		int p = pos;
		while (p < end && (arrPos < arrMax || state == 7) && (interrupt == null || p - init < min || !interrupt.get())) {
			switch (state) {
			case 0:
				if (arr[arrPos] >= 0) {
					// 0aaa aaaa
					chars[p++] = (char)arr[arrPos++];
					int max = p + arrMax - arrPos;
					if (max > end) max = end;
					while (p < max && arr[arrPos] >= 0)
						chars[p++] = (char)arr[arrPos++];
					if (p == max) {
						b.position(arrPos - b.arrayOffset());
						return p - init;
					}
				}
				a = arr[arrPos++];
				if ((a & 0xE0) == 0xC0) {
					// 110a aaaa 10bb bbbb
					val = (a & 0x1F) << 6;
					state = 1;
				} else if ((a & 0xF0) == 0xE0) {
					// 1110 aaaa 10bb bbbb 10cc cccc
					val = (a & 0x0F) << 12;
					state = 2;
				} else if ((a & 0xF8) == 0xF0) {
					// 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
					val = (a & 0x07) << 12;
					state = 4;
				} else {
					chars[p++] = INVALID_CHAR;
				}
				break;
			case 1: case 3:
				// 1: second byte of 110a aaaa 10bb bbbb
				// 3: third byte of 1110 aaaa 10bb bbbb 10cc cccc
				if (((a = arr[arrPos++]) & 0xC0) == 0x80) {
					chars[p++] = (char)(val | (a & 0x3F));
				} else {
					chars[p++] = INVALID_CHAR;
				}
				state = 0;
				break;
			case 2:
				// second byte of 1110 aaaa 10bb bbbb 10cc cccc
				if (((a = arr[arrPos++]) & 0xC0) == 0x80) {
					val |= (a & 0x3F) << 6;
					state = 3;
				} else {
					chars[p++] = INVALID_CHAR;
					state = 0;
				}
				break;
			case 4:
				// second byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
				if (((a = arr[arrPos++]) & 0xC0) == 0x80) {
					val |= (a & 0x3F) << 6;
					state = 5;
				} else {
					chars[p++] = INVALID_CHAR;
					state = 0;
				}
				break;
			case 5:
				// third byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
				if (((a = arr[arrPos++]) & 0xC0) == 0x80) {
					val |= (a & 0x3F);
					state = 6;
				} else {
					chars[p++] = INVALID_CHAR;
					state = 0;
				}
				break;
			case 6:
				// fourth byte of 1111 0aaa 10bb bbbb 10cc cccc 10dd dddd
				if (((a = arr[arrPos++]) & 0xC0) == 0x80) {
					// UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
					// Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
					//          [1101 11yy] [yyxx xxxx] (low surrogate)
					//          * uuuuu = wwww + 1
					int uuuuu = (val & 0x7C00) >> 10;
					// TODO if > 0x10 invalid Surrogate
					int wwww = uuuuu - 1;
					chars[p++] = (char)(0xD800 | (wwww << 6) | ((val & 0x3C0) >> 4) | ((val & 0x30) >> 4));
					val = (0xDC00 | ((val & 0xF) << 6) | (a & 0x3F));
					state = 7;
				} else {
					chars[p++] = INVALID_CHAR;
					state = 0;
				}
				break;
			case 7:
				chars[p++] = (char)val;
				state = 0;
				break;
			}
		}
		b.position(arrPos - b.arrayOffset());
		return p - init;
	}

	@Override
	public Charset getEncoding() {
		return StandardCharsets.UTF_8;
	}

}
