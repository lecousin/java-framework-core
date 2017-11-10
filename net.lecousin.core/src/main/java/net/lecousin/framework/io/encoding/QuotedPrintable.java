package net.lecousin.framework.io.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.util.StringUtil;

/** Utility method to encode and decode quoted-printable as defined in RFC 2045. */
public final class QuotedPrintable {

	/** Decode the given input. */
	public static ByteBuffer decode(ByteBuffer input) throws IOException {
		byte[] out = new byte[input.remaining()];
		int pos = 0;
		int endingSpaces = 0;
		while (input.hasRemaining()) {
			byte b = input.get();
			if ((b >= 33 && b <= 60) || (b >= 62 && b <= 126)) {
				out[pos++] = b;
				endingSpaces = 0;
				continue;
			}
			if (b == '=') {
				if (input.remaining() < 2) {
					// not enough data to read
					input.position(input.position() - 1);
					break;
				}
				int i = StringUtil.decodeHexa((char)(input.get() & 0xFF));
				if (i == -1) throw new IOException("Invalid hexadecimal value in quoted-printable");
				int j = StringUtil.decodeHexa((char)(input.get() & 0xFF));
				if (j == -1) throw new IOException("Invalid hexadecimal value in quoted-printable");
				out[pos++] = (byte)((i << 4) | j);
				endingSpaces = 0;
				continue;
			}
			if (b == ' ' || b == '\t') {
				out[pos++] = b;
				endingSpaces++;
				continue;
			}
			if (b == '\r') {
				endingSpaces++;
				continue;
			}
			if (b == '\n') {
				// end of line
				pos -= endingSpaces;
				endingSpaces = 0;
				continue;
			}
			throw new IOException("Unexpected byte " + b + " in quoted-printable data");
		}
		pos -= endingSpaces;
		input.position(input.position() - endingSpaces);
		return ByteBuffer.wrap(out, 0, pos);
	}

	/** Decode the given input. */
	public static ByteBuffer decode(byte[] input) throws IOException {
		return decode(ByteBuffer.wrap(input));
	}
	
	/** Decode the given input. */
	public static ByteBuffer decode(String input) throws IOException {
		return decode(input.getBytes(StandardCharsets.US_ASCII));
	}
	
	/** Encode the given input. */
	public static ByteBuffer encode(String text, Charset charset) {
		return encode(text.getBytes(charset));
	}
	
	/** Encode the given input. */
	public static ByteBuffer encode(byte[] input) {
		int pos = input.length + input.length / 3;
		if (pos < 64) pos = 64;
		byte[] out = new byte[pos];
		pos = 0;
		int lineCount = 0;
		for (int i = 0; i < input.length; ++i) {
			byte b = input[i];
			if ((b >= 33 && b <= 60) || (b >= 62 && b <= 126)) {
				if (lineCount == 76) {
					if (pos >= out.length - 3)
						out = increaseBuffer(out, pos);
					out[pos++] = '\r';
					out[pos++] = '\n';
					lineCount = 0;
				} else
					if (pos >= out.length - 1)
						out = increaseBuffer(out, pos);
				out[pos++] = b;
				lineCount++;
				continue;
			}
			// need to encode
			if (lineCount >= 76 - 3) {
				if (pos >= out.length - 6)
					out = increaseBuffer(out, pos);
				out[pos++] = '\r';
				out[pos++] = '\n';
				lineCount = 0;
			}
			out[pos++] = '=';
			out[pos++] = (byte)StringUtil.encodeHexaDigit((b & 0xF0) >> 4);
			out[pos++] = (byte)StringUtil.encodeHexaDigit(b & 0xF);
		}
		return ByteBuffer.wrap(out, 0, pos);
	}
	
	private static byte[] increaseBuffer(byte[] b, int len) {
		byte[] nb = new byte[b.length + b.length / 3];
		System.arraycopy(b, 0, nb, 0, len);
		return nb;
	}
	
}
