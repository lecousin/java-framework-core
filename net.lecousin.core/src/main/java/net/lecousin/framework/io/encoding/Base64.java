package net.lecousin.framework.io.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.io.text.ICharacterStream;

/** Utility methods to encode and decode base 64. */
public final class Base64 {
	
	private Base64() { /* no instance */ }

	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes. */
	public static int decode4BytesBase64(byte[] inputBuffer, byte[] outputBuffer) throws IOException {
		return decode4BytesBase64(inputBuffer, 0, outputBuffer, 0);
	}

	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public static int decode4BytesBase64(byte[] inputBuffer, byte[] outputBuffer, int outputOffset) throws IOException {
		return decode4BytesBase64(inputBuffer, 0, outputBuffer, outputOffset);
	}

	/** Decode 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public static int decode4BytesBase64(byte[] inputBuffer, int inputOffset, byte[] outputBuffer, int outputOffset) throws IOException {
		int v1 = decodeBase64Char(inputBuffer[inputOffset]);
		int v2 = decodeBase64Char(inputBuffer[inputOffset + 1]);
		int v3 = decodeBase64Char(inputBuffer[inputOffset + 2]);
		outputBuffer[outputOffset] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) return 1;
		int v4 = decodeBase64Char(inputBuffer[inputOffset + 3]);
		outputBuffer[outputOffset + 1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[outputOffset + 2] = (byte)(((v3 & 0x03) << 6) | v4);
		return 3;
	}
	
	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public static int decode4BytesBase64(ByteBuffer inputBuffer, byte[] outputBuffer, int outputOffset) throws IOException {
		int v1 = decodeBase64Char(inputBuffer.get());
		int v2 = decodeBase64Char(inputBuffer.get());
		int v3 = decodeBase64Char(inputBuffer.get());
		outputBuffer[outputOffset] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) return 1;
		int v4 = decodeBase64Char(inputBuffer.get());
		outputBuffer[outputOffset + 1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[outputOffset + 2] = (byte)(((v3 & 0x03) << 6) | v4);
		return 3;
	}

	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public static int decode4BytesBase64(CharBuffer inputBuffer, byte[] outputBuffer, int outputOffset) throws IOException {
		int v1 = decodeBase64Char((byte)inputBuffer.get());
		int v2 = decodeBase64Char((byte)inputBuffer.get());
		int v3 = decodeBase64Char((byte)inputBuffer.get());
		outputBuffer[outputOffset] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) return 1;
		int v4 = decodeBase64Char((byte)inputBuffer.get());
		outputBuffer[outputOffset + 1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[outputOffset + 2] = (byte)(((v3 & 0x03) << 6) | v4);
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
	
	/** Decode the given input. */
	public static byte[] decode(byte[] input) throws IOException {
		int l = input.length;
		if ((l % 4) != 0) l = (l / 4) * 4;
		if (l == 0) return new byte[0];
		int outLen = l * 3 / 4;
		if (input[l - 1] == '=') {
			if (input[l - 2] == '=')
				outLen -= 2;
			else
				outLen -= 1;
		}
		byte[] decoded = new byte[outLen];
		int pos = 0;
		for (int i = 0; i < l; i += 4, pos += 3)
			decode4BytesBase64(input, i, decoded, pos);
		return decoded;
	}
	
	/** Decode the given input. */
	public static byte[] decode(ByteBuffer input) throws IOException {
		int l = input.remaining();
		if ((l % 4) != 0) l = (l / 4) * 4;
		if (l == 0) return new byte[0];
		int outLen = l * 3 / 4;
		int p = input.position();
		if (input.get(p + l - 1) == '=') {
			if (input.get(p + l - 2) == '=')
				outLen -= 2;
			else
				outLen -= 1;
		}
		byte[] decoded = new byte[outLen];
		int pos = 0;
		for (int i = 0; i < l; i += 4, pos += 3)
			decode4BytesBase64(input, decoded, pos);
		return decoded;
	}
	
	/** Decode the given input. */
	public static byte[] decode(CharBuffer input) throws IOException {
		int l = input.remaining();
		if ((l % 4) != 0) l = (l / 4) * 4;
		if (l == 0) return new byte[0];
		int outLen = l * 3 / 4;
		int p = input.position();
		if (input.get(p + l - 1) == '=') {
			if (input.get(p + l - 2) == '=')
				outLen -= 2;
			else
				outLen -= 1;
		}
		byte[] decoded = new byte[outLen];
		int pos = 0;
		for (int i = 0; i < l; i += 4, pos += 3)
			decode4BytesBase64(input, decoded, pos);
		return decoded;
	}
	
	/** Decode the given input. */
	public static byte[] decode(String input) throws IOException {
		return decode(input.getBytes(StandardCharsets.US_ASCII));
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

	/** Encode the 3 bytes from inputBuffer, into 4 bytes in the outputBuffer. */
	public static void encode3BytesBase64(byte[] inputBuffer, int inputBufferOffset, char[] outputBuffer, int outputBufferOffset) {
		try {
			outputBuffer[outputBufferOffset + 0] = (char)encodeBase64((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
			outputBuffer[outputBufferOffset + 1] =
				(char)encodeBase64(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4)
								| ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4));
			outputBuffer[outputBufferOffset + 2] =
				(char)encodeBase64(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2)
								| ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6));
			outputBuffer[outputBufferOffset + 3] = (char)encodeBase64(inputBuffer[inputBufferOffset + 2] & 0x3F);
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

	/** Encode 1 to 3 bytes. */
	public static void encodeUpTo3BytesBase64(
		byte[] inputBuffer, int inputBufferOffset, char[] outputBuffer, int outputBufferOffset, int nbInput
	) {
		try {
			outputBuffer[outputBufferOffset + 0] = (char)encodeBase64((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
			if (nbInput == 1) {
				outputBuffer[outputBufferOffset + 1] = (char)encodeBase64(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4));
				outputBuffer[outputBufferOffset + 2] = '=';
				outputBuffer[outputBufferOffset + 3] = '=';
			} else {
				outputBuffer[outputBufferOffset + 1] = (char)encodeBase64(
					  ((inputBuffer[inputBufferOffset + 0] & 0x03) << 4)
					| ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4)
				);
				if (nbInput == 2) {
					outputBuffer[outputBufferOffset + 2] = (char)encodeBase64(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2));
					outputBuffer[outputBufferOffset + 3] = '=';
				} else {
					outputBuffer[outputBufferOffset + 2] = (char)encodeBase64(
						  ((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2)
						| ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6)
					);
					outputBuffer[outputBufferOffset + 3] = (char)encodeBase64(inputBuffer[inputBufferOffset + 2] & 0x3F);
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
	
	/** Encode the given bytes into base 64. */
	public static char[] encodeBase64ToChars(byte[] input, int offset, int len) {
		int nb = len / 3;
		if ((len % 3) > 0) nb++;
		nb *= 4;
		char[] output = new char[nb];
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
	
	/** Encode the Readable IO into the Writer. */
	@SuppressWarnings("resource")
	public static ISynchronizationPoint<IOException> encodeAsync(IO.Readable io, IO.WriterAsync writer) {
		return encodeAsync(io instanceof IO.Readable.Buffered ? (IO.Readable.Buffered)io : new SimpleBufferedReadable(io, 8192), writer);
	}

	/** Encode the Readable IO into the Writer. */
	public static ISynchronizationPoint<IOException> encodeAsync(IO.Readable.Buffered io, IO.WriterAsync writer) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		encodeAsyncNextBuffer(io, writer, result, new byte[3], 0, null);
		return result;
	}
	
	/** Encode the Readable IO into the Writer. */
	@SuppressWarnings("resource")
	public static ISynchronizationPoint<IOException> encodeAsync(IO.Readable io, ICharacterStream.WriterAsync writer) {
		return encodeAsync(io instanceof IO.Readable.Buffered ? (IO.Readable.Buffered)io : new SimpleBufferedReadable(io, 8192), writer);
	}
	
	/** Encode the Readable IO into the Writer. */
	public static ISynchronizationPoint<IOException> encodeAsync(IO.Readable.Buffered io, ICharacterStream.WriterAsync writer) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		encodeAsyncNextBuffer(io, writer, result, new byte[3], 0, null);
		return result;
	}

	private static void encodeAsyncNextBuffer(
		IO.Readable.Buffered io, IO.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ISynchronizationPoint<IOException> lastWrite
	) {
		io.readNextBufferAsync().listenInline((buffer) -> {
			if (buffer == null)
				writeFinalBuffer(io, writer, result, buf, nbBuf, lastWrite);
			else
				writeBuffer(io, writer, result, buf, nbBuf, buffer, lastWrite);
		}, result);
	}
	
	private static void encodeAsyncNextBuffer(
		IO.Readable.Buffered io, ICharacterStream.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ISynchronizationPoint<IOException> lastWrite
	) {
		io.readNextBufferAsync().listenInline((buffer) -> {
			if (buffer == null)
				writeFinalBuffer(io, writer, result, buf, nbBuf, lastWrite);
			else
				writeBuffer(io, writer, result, buf, nbBuf, buffer, lastWrite);
		}, result);
	}

	// skip checkstyle: VariableDeclarationUsageDistance
	private static void writeBuffer(
		IO.Readable.Buffered io, IO.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ByteBuffer buffer, ISynchronizationPoint<IOException> lastWrite
	) {
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Encode base 64 stream", io.getPriority()) {
			@Override
			public Void run() {
				if (lastWrite != null) {
					if (lastWrite.hasError()) {
						result.error(lastWrite.getError());
						return null;
					}
					if (lastWrite.isCancelled()) {
						result.cancel(lastWrite.getCancelEvent());
						return null;
					}
				}
				int nb = nbBuf;
				if (nb > 0) {
					while (nb < 3 && buffer.hasRemaining())
						buf[nb++] = buffer.get();
					if (nb == 3) {
						byte[] out = new byte[4];
						encode3BytesBase64(buf, 0, out, 0);
						writeBuffer(io, writer, result, buf, 0, buffer, writer.writeAsync(ByteBuffer.wrap(out)));
						return null;
					}
					if (nb < 3) {
						encodeAsyncNextBuffer(io, writer, result, buf, nb, lastWrite);
						return null;
					}
				}
				int l = buffer.remaining() / 3;
				byte[] out = encodeBase64(buffer.array(), buffer.position(), l * 3);
				ISynchronizationPoint<IOException> write = writer.writeAsync(ByteBuffer.wrap(out));
				buffer.position(buffer.position() + l * 3);
				nb = buffer.remaining();
				buffer.get(buf, 0, nb);
				encodeAsyncNextBuffer(io, writer, result, buf, nb, write);
				return null;
			}
		};
		if (lastWrite == null || lastWrite.isUnblocked()) task.start();
		else task.startOn(lastWrite, true);
	}
	
	private static void writeBuffer(
		IO.Readable.Buffered io, ICharacterStream.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ByteBuffer buffer, ISynchronizationPoint<IOException> lastWrite
	) {
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Encode base 64 stream", io.getPriority()) {
			@Override
			public Void run() {
				if (lastWrite != null) {
					if (lastWrite.hasError()) {
						result.error(lastWrite.getError());
						return null;
					}
					if (lastWrite.isCancelled()) {
						result.cancel(lastWrite.getCancelEvent());
						return null;
					}
				}
				int nb = nbBuf;
				if (nb > 0) {
					while (nb < 3 && buffer.hasRemaining())
						buf[nb++] = buffer.get();
					if (nb == 3) {
						char[] out = new char[4];
						encode3BytesBase64(buf, 0, out, 0);
						writeBuffer(io, writer, result, buf, 0, buffer, writer.writeAsync(out, 0, 4));
						return null;
					}
					if (nb < 3) {
						encodeAsyncNextBuffer(io, writer, result, buf, nb, lastWrite);
						return null;
					}
				}
				int l = buffer.remaining() / 3;
				char[] out = encodeBase64ToChars(buffer.array(), buffer.position(), l * 3);
				ISynchronizationPoint<IOException> write = writer.writeAsync(out);
				buffer.position(buffer.position() + l * 3);
				nb = buffer.remaining();
				buffer.get(buf, 0, nb);
				encodeAsyncNextBuffer(io, writer, result, buf, nb, write);
				return null;
			}
		};
		if (lastWrite == null || lastWrite.isUnblocked()) task.start();
		else task.startOn(lastWrite, true);
	}

	private static void writeFinalBuffer(
		IO.Readable.Buffered io, IO.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ISynchronizationPoint<IOException> lastWrite
	) {
		if (nbBuf == 0) {
			if (lastWrite == null || lastWrite.isUnblocked())
				result.unblock();
			else
				lastWrite.listenInline(result);
			return;
		}
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Encode base 64 stream", io.getPriority()) {
			@Override
			public Void run() {
				if (lastWrite != null) {
					if (lastWrite.hasError()) {
						result.error(lastWrite.getError());
						return null;
					}
					if (lastWrite.isCancelled()) {
						result.cancel(lastWrite.getCancelEvent());
						return null;
					}
				}
				byte[] out = new byte[4];
				encodeUpTo3BytesBase64(buf, 0, out, 0, nbBuf);
				writer.writeAsync(ByteBuffer.wrap(out)).listenInline(result);
				return null;
			}
		};
		if (lastWrite == null || lastWrite.isUnblocked()) task.start();
		else task.startOn(lastWrite, true);
	}

	private static void writeFinalBuffer(
		IO.Readable.Buffered io, ICharacterStream.WriterAsync writer, SynchronizationPoint<IOException> result,
		byte[] buf, int nbBuf, ISynchronizationPoint<IOException> lastWrite
	) {
		if (nbBuf == 0) {
			if (lastWrite == null || lastWrite.isUnblocked())
				result.unblock();
			else
				lastWrite.listenInline(result);
			return;
		}
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Encode base 64 stream", io.getPriority()) {
			@Override
			public Void run() {
				if (lastWrite != null) {
					if (lastWrite.hasError()) {
						result.error(lastWrite.getError());
						return null;
					}
					if (lastWrite.isCancelled()) {
						result.cancel(lastWrite.getCancelEvent());
						return null;
					}
				}
				char[] out = new char[4];
				encodeUpTo3BytesBase64(buf, 0, out, 0, nbBuf);
				writer.writeAsync(out, 0, 4).listenInline(result);
				return null;
			}
		};
		if (lastWrite == null || lastWrite.isUnblocked()) task.start();
		else task.startOn(lastWrite, true);
	}
	
}
