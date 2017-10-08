package net.lecousin.framework.io.encoding;

import java.io.IOException;

/** Utility methods to encode and decode base 64. */
public final class Base64 {
	
	private Base64() { /* no instance */ }

	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes. */
	public static int decode4BytesBase64(byte[] inputBuffer, byte[] outputBuffer) throws IOException {
		int v1 = decodeBase64Char(inputBuffer[0]);
		int v2 = decodeBase64Char(inputBuffer[1]);
		int v3 = decodeBase64Char(inputBuffer[2]);
		outputBuffer[0] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) return 1;
		int v4 = decodeBase64Char(inputBuffer[3]);
		outputBuffer[1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[2] = (byte)(((v3 & 0x03) << 6) | v4);
		return 3;
	}
	
	/** Convert a base 64 character into its integer value. */
	public static int decodeBase64Char(byte b) throws IOException {
		if (b >= 'A' && b <= 'Z') return b - 'A';
		if (b >= 'a' && b <= 'z') return b - 'a' + 26;
		if (b >= '0' && b <= '9') return b - '0' + 52;
		if (b == '+') return 62;
		if (b == '/') return 63;
		if (b == '=') return 64;
		throw new IOException("Invalid Base64 character to decode: " + b);
	}
	
	/** Encode the 3 bytes from inputBuffer, into 4 bytes in the outputBuffer. */
	public static void encode3BytesBase64(byte[] inputBuffer, int inputBufferOffset, byte[] outputBuffer, int outputBufferOffset) {
		try {
			outputBuffer[outputBufferOffset + 0] = encodeBase64((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
			outputBuffer[outputBufferOffset + 1] =
				encodeBase64(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4) | ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4));
			outputBuffer[outputBufferOffset + 2] =
				encodeBase64(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2) | ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6));
			outputBuffer[outputBufferOffset + 3] = encodeBase64(inputBuffer[inputBufferOffset + 2] & 0x3F);
		} catch (IOException e) {
			// not possible
		}
	}
	
	/** Encode 1 to 3 bytes. */
	public static void encodeUpTo3BytesBase64(
		byte[] inputBuffer, int inputBufferOffset, byte[] outputBuffer, int outputBufferOffset, int nbInput
	) {
		try {
			outputBuffer[outputBufferOffset + 0] = encodeBase64((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
			if (nbInput == 1) {
				outputBuffer[outputBufferOffset + 1] = encodeBase64(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4));
				outputBuffer[outputBufferOffset + 2] = '=';
				outputBuffer[outputBufferOffset + 3] = '=';
			} else {
				outputBuffer[outputBufferOffset + 1] = encodeBase64(
					  ((inputBuffer[inputBufferOffset + 0] & 0x03) << 4)
					| ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4)
				);
				if (nbInput == 2) {
					outputBuffer[outputBufferOffset + 2] = encodeBase64(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2));
					outputBuffer[outputBufferOffset + 3] = '=';
				} else {
					outputBuffer[outputBufferOffset + 2] = encodeBase64(
						  ((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2)
						| ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6)
					);
					outputBuffer[outputBufferOffset + 3] = encodeBase64(inputBuffer[inputBufferOffset + 2] & 0x3F);
				}
			}
		} catch (IOException e) {
			// not possible
		}
	}
	
	/** Encode the given integer (from 0 to 63) into its base 64 character. */
	public static byte encodeBase64(int v) throws IOException {
		if (v <= 25) return (byte)(v + 'A');
		if (v <= 51) return (byte)(v - 26 + 'a');
		if (v <= 61) return (byte)(v - 52 + '0');
		if (v == 62) return (byte)'+';
		if (v == 63) return (byte)'/';
		throw new IOException("Invalid Base64 value to encode: " + v);
	}
	
	/** Encode the given bytes into base 64. */
	public static byte[] encodeBase64(byte[] input) {
		return encodeBase64(input, 0, input.length);
	}

	/** Encode the given bytes into base 64. */
	public static byte[] encodeBase64(byte[] input, int offset, int len) {
		int nb = len / 3;
		if ((len % 3) > 0) nb++;
		nb *= 4;
		byte[] output = new byte[nb];
		int pos = 0;
		while (offset + 3 < len) {
			encode3BytesBase64(input, offset, output, pos);
			offset += 3;
			pos += 4;
		}
		if (offset < len)
			encodeUpTo3BytesBase64(input, offset, output, pos, len - offset);
		return output;
	}
		
}
