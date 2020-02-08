package net.lecousin.framework.encoding;

import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.memory.ByteArrayCache;

/** Encode and decode base 64. */
public final class Base64Encoding implements BytesEncoder.KnownOutputSize, BytesDecoder.KnownOutputSize {
	
	/** base64 instance. */
	public static final Base64Encoding instance = new Base64Encoding((byte)'+', (byte)'/');
	/** base64url instance. */
	public static final Base64Encoding instanceURL = new Base64Encoding((byte)'-', (byte)'_');
	
	private Base64Encoding(byte char62, byte char63) {
		this.char62 = char62;
		this.char63 = char63;
	}
	
	private byte char62;
	private byte char63;
	
	/** Error trying to decode base 64 with an invalid character. */
	public static final class InvalidBase64Character extends EncodingException {
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public InvalidBase64Character(byte b) {
			super("Invalid base 64 character: " + (b & 0xFF));
		}
	}

	/** Error trying to encode a value into base 64 (value must be between 0 and 63). */
	public static final class InvalidBase64Value extends EncodingException {
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public InvalidBase64Value(int value) {
			super("Invalid base 64 value: " + value);
		}
	}

	/** Convert a base 64 character into its integer value, 64 is returned for '='. */
	public int decodeChar(byte b) throws EncodingException {
		if (b >= 'A') {
			if (b >= 'a') {
				if (b <= 'z') return b - 'a' + 26;
				throw new InvalidBase64Character(b);
			}
			if (b <= 'Z') return b - 'A';
			throw new InvalidBase64Character(b);
		}
		if (b >= '0') {
			if (b <= '9') return b - '0' + 52;
			if (b == '=') return 64;
			throw new InvalidBase64Character(b);
		}
		if (b == char62) return 62;
		if (b == char63) return 63;
		throw new InvalidBase64Character(b);
	}

	/** Decode 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public int decode4Bytes(byte[] inputBuffer, int inputOffset, byte[] outputBuffer, int outputOffset) throws EncodingException {
		int v1 = decodeChar(inputBuffer[inputOffset]);
		int v2 = decodeChar(inputBuffer[inputOffset + 1]);
		int v3 = decodeChar(inputBuffer[inputOffset + 2]);
		outputBuffer[outputOffset] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) return 1;
		int v4 = decodeChar(inputBuffer[inputOffset + 3]);
		outputBuffer[outputOffset + 1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[outputOffset + 2] = (byte)(((v3 & 0x03) << 6) | v4);
		return 3;
	}
	
	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public int decode4Bytes(Bytes.Readable inputBuffer, byte[] outputBuffer, int outputOffset) throws EncodingException {
		int v1 = decodeChar(inputBuffer.get());
		int v2 = decodeChar(inputBuffer.get());
		int v3 = decodeChar(inputBuffer.get());
		outputBuffer[outputOffset] = (byte)((v1 << 2) | (v2 >>> 4));
		if (v3 == 64) {
			// eat the last = if present
			if (inputBuffer.hasRemaining() && inputBuffer.getForward(0) == '=')
				inputBuffer.moveForward(1);
			return 1;
		}
		int v4 = decodeChar(inputBuffer.get());
		outputBuffer[outputOffset + 1] = (byte)(((v2 & 0x0F) << 4) | (v3 >>> 2));
		if (v4 == 64) return 2;
		outputBuffer[outputOffset + 2] = (byte)(((v3 & 0x03) << 6) | v4);
		return 3;
	}

	/** Decode 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public int decode4Bytes(byte[] inputBuffer, int inputOffset, Bytes.Writable outputBuffer) throws EncodingException {
		int v1 = decodeChar(inputBuffer[inputOffset]);
		int v2 = decodeChar(inputBuffer[inputOffset + 1]);
		int v3 = decodeChar(inputBuffer[inputOffset + 2]);
		outputBuffer.put((byte)((v1 << 2) | (v2 >>> 4)));
		if (v3 == 64) return 1;
		int v4 = decodeChar(inputBuffer[inputOffset + 3]);
		outputBuffer.put((byte)(((v2 & 0x0F) << 4) | (v3 >>> 2)));
		if (v4 == 64) return 2;
		outputBuffer.put((byte)(((v3 & 0x03) << 6) | v4));
		return 3;
	}
	
	/** Decode the 4 input bytes of the inputBuffer, into the outputBuffer that must have at least 3 bytes remaining. */
	public int decode4Bytes(Bytes.Readable inputBuffer, Bytes.Writable outputBuffer) throws EncodingException {
		int v1 = decodeChar(inputBuffer.get());
		int v2 = decodeChar(inputBuffer.get());
		int v3 = decodeChar(inputBuffer.get());
		outputBuffer.put((byte)((v1 << 2) | (v2 >>> 4)));
		if (v3 == 64) {
			// eat the last = if present
			if (inputBuffer.hasRemaining() && inputBuffer.getForward(0) == '=')
				inputBuffer.moveForward(1);
			return 1;
		}
		int v4 = decodeChar(inputBuffer.get());
		outputBuffer.put((byte)(((v2 & 0x0F) << 4) | (v3 >>> 2)));
		if (v4 == 64) return 2;
		outputBuffer.put((byte)(((v3 & 0x03) << 6) | v4));
		return 3;
	}
	
	/** Decode the given input. */
	@Override
	public byte[] decode(byte[] input, int offset, int length) throws EncodingException {
		if (length == 0) return new byte[0];
		int nbBlocks = length / 4;
		int lastBlock = length % 4;
		int outLen;
		if (lastBlock == 0) {
			outLen = nbBlocks * 3;
			if (input[offset + nbBlocks * 4 - 1] == '=') {
				if (input[offset + nbBlocks * 4 - 2] == '=')
					outLen -= 2;
				else
					outLen -= 1;
			}
		} else if (lastBlock == 3) {
			if (input[offset + nbBlocks * 4 + 2] != '=')
				throw new UnexpectedEndOfEncodedData();
			outLen = nbBlocks * 3 + 1;
			nbBlocks++;
		} else {
			throw new UnexpectedEndOfEncodedData();
		}
		
		byte[] decoded = new byte[outLen];
		int pos = 0;
		for (int i = 0; i < nbBlocks; i++, pos += 3)
			decode4Bytes(input, offset + i * 4, decoded, pos);
		return decoded;
	}
	
	/** Decode the given input. */
	@Override
	public byte[] decode(Bytes.Readable input) throws EncodingException {
		int length = input.remaining();
		if (length == 0) return new byte[0];
		int nbBlocks = length / 4;
		int lastBlock = length % 4;
		int outLen;
		if (lastBlock == 0) {
			outLen = nbBlocks * 3;
			if (input.getForward(nbBlocks * 4 - 1) == '=') {
				if (input.getForward(nbBlocks * 4 - 2) == '=')
					outLen -= 2;
				else
					outLen -= 1;
			}
		} else if (lastBlock == 3) {
			if (input.getForward(nbBlocks * 4 + 2) != '=')
				throw new UnexpectedEndOfEncodedData();
			outLen = nbBlocks * 3 + 1;
			nbBlocks++;
		} else {
			throw new UnexpectedEndOfEncodedData();
		}
		
		byte[] decoded = new byte[outLen];
		int pos = 0;
		for (int i = 0; i < nbBlocks; i++, pos += 3)
			decode4Bytes(input, decoded, pos);
		return decoded;
	}
	
	@Override
	public void decode(Bytes.Readable input, Bytes.Writable output, boolean end) throws EncodingException {
		int l = Math.min(input.remaining() / 4, output.remaining() / 3);
		for (int i = 0; i < l; ++i)
			decode4Bytes(input, output);
		if (end) {
			int remIn = input.remaining();
			if (remIn == 0)
				return;
			if (remIn < 3)
				throw new UnexpectedEndOfEncodedData();
			if (!output.hasRemaining())
				return;

			int remDec;
			if (input.getForward(2) == '=')
				remDec = 1;
			else if (remIn >= 4) {
				if (input.getForward(3) == '=')
					remDec = 2;
				else
					remDec = 3;
			} else {
				throw new UnexpectedEndOfEncodedData();
			}
			if (remDec > output.remaining())
				return;
			decode4Bytes(input, output);
		}
	}
	
	@Override
	public <TError extends Exception> AsyncConsumer<Bytes.Readable, TError> createDecoderConsumer(
		AsyncConsumer<Bytes.Readable, TError> decodedConsumer, Function<EncodingException, TError> errorConverter
	) {
		return new DecoderConsumer<>(decodedConsumer, errorConverter);
	}
	
	/** Encode the 3 bytes from inputBuffer, into 4 bytes in the outputBuffer. */
	public void encode3Bytes(byte[] inputBuffer, int inputBufferOffset, byte[] outputBuffer, int outputBufferOffset) {
		outputBuffer[outputBufferOffset + 0] = encodeSafe((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
		outputBuffer[outputBufferOffset + 1] =
			encodeSafe(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4) | ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4));
		outputBuffer[outputBufferOffset + 2] =
			encodeSafe(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2) | ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6));
		outputBuffer[outputBufferOffset + 3] = encodeSafe(inputBuffer[inputBufferOffset + 2] & 0x3F);
	}
	
	/** Encode the 3 bytes from inputBuffer, into 4 bytes in the outputBuffer. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public void encode3Bytes(Bytes.Readable inputBuffer, byte[] outputBuffer, int outputBufferOffset) {
		byte b;
		outputBuffer[outputBufferOffset + 0] = encodeSafe(((b = inputBuffer.get()) & 0xFC) >> 2);
		outputBuffer[outputBufferOffset + 1] =
			encodeSafe(((b & 0x03) << 4) | (((b = inputBuffer.get()) & 0xF0) >> 4));
		outputBuffer[outputBufferOffset + 2] =
			encodeSafe(((b & 0x0F) << 2) | (((b = inputBuffer.get()) & 0xC0) >> 6));
		outputBuffer[outputBufferOffset + 3] = encodeSafe(b & 0x3F);
	}
	
	/** Encode the 3 bytes from inputBuffer, into 4 bytes in the outputBuffer. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public void encode3Bytes(Bytes.Readable inputBuffer, Bytes.Writable outputBuffer) {
		byte b;
		outputBuffer.put(encodeSafe(((b = inputBuffer.get()) & 0xFC) >> 2));
		outputBuffer.put(encodeSafe(((b & 0x03) << 4) | (((b = inputBuffer.get()) & 0xF0) >> 4)));
		outputBuffer.put(encodeSafe(((b & 0x0F) << 2) | (((b = inputBuffer.get()) & 0xC0) >> 6)));
		outputBuffer.put(encodeSafe(b & 0x3F));
	}

	/** Encode 1 to 3 bytes. */
	public void encodeUpTo3Bytes(
		byte[] inputBuffer, int inputBufferOffset, byte[] outputBuffer, int outputBufferOffset, int nbInput
	) {
		outputBuffer[outputBufferOffset + 0] = encodeSafe((inputBuffer[inputBufferOffset + 0] & 0xFC) >> 2);
		if (nbInput == 1) {
			outputBuffer[outputBufferOffset + 1] = encodeSafe(((inputBuffer[inputBufferOffset + 0] & 0x03) << 4));
			outputBuffer[outputBufferOffset + 2] = '=';
			outputBuffer[outputBufferOffset + 3] = '=';
			return;
		}
		outputBuffer[outputBufferOffset + 1] = encodeSafe(
			  ((inputBuffer[inputBufferOffset + 0] & 0x03) << 4)
			| ((inputBuffer[inputBufferOffset + 1] & 0xF0) >> 4)
		);
		if (nbInput == 2) {
			outputBuffer[outputBufferOffset + 2] = encodeSafe(((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2));
			outputBuffer[outputBufferOffset + 3] = '=';
			return;
		}
		outputBuffer[outputBufferOffset + 2] = encodeSafe(
			  ((inputBuffer[inputBufferOffset + 1] & 0x0F) << 2)
			| ((inputBuffer[inputBufferOffset + 2] & 0xC0) >> 6)
		);
		outputBuffer[outputBufferOffset + 3] = encodeSafe(inputBuffer[inputBufferOffset + 2] & 0x3F);
	}

	/** Encode 1 to 3 bytes. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public void encodeUpTo3Bytes(Bytes.Readable inputBuffer, byte[] outputBuffer, int outputBufferOffset) {
		int nbInput = inputBuffer.remaining();
		byte b;
		outputBuffer[outputBufferOffset + 0] = encodeSafe(((b = inputBuffer.get()) & 0xFC) >> 2);
		if (nbInput == 1) {
			outputBuffer[outputBufferOffset + 1] = encodeSafe(((b & 0x03) << 4));
			outputBuffer[outputBufferOffset + 2] = '=';
			outputBuffer[outputBufferOffset + 3] = '=';
			return;
		}
		outputBuffer[outputBufferOffset + 1] = encodeSafe(((b & 0x03) << 4) | (((b = inputBuffer.get()) & 0xF0) >> 4));
		if (nbInput == 2) {
			outputBuffer[outputBufferOffset + 2] = encodeSafe(((b & 0x0F) << 2));
			outputBuffer[outputBufferOffset + 3] = '=';
			return;
		}
		outputBuffer[outputBufferOffset + 2] = encodeSafe(((b & 0x0F) << 2) | (((b = inputBuffer.get()) & 0xC0) >> 6));
		outputBuffer[outputBufferOffset + 3] = encodeSafe(b & 0x3F);
	}

	/** Encode 1 to 3 bytes. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public void encodeUpTo3Bytes(Bytes.Readable inputBuffer, Bytes.Writable outputBuffer) {
		int nbInput = inputBuffer.remaining();
		byte b;
		outputBuffer.put(encodeSafe(((b = inputBuffer.get()) & 0xFC) >> 2));
		if (nbInput == 1) {
			outputBuffer.put(encodeSafe(((b & 0x03) << 4)));
			outputBuffer.put((byte)'=');
			outputBuffer.put((byte)'=');
			return;
		}
		outputBuffer.put(encodeSafe(((b & 0x03) << 4) | (((b = inputBuffer.get()) & 0xF0) >> 4)));
		if (nbInput == 2) {
			outputBuffer.put(encodeSafe(((b & 0x0F) << 2)));
			outputBuffer.put((byte)'=');
			return;
		}
		outputBuffer.put(encodeSafe(((b & 0x0F) << 2) | (((b = inputBuffer.get()) & 0xC0) >> 6)));
		outputBuffer.put(encodeSafe(b & 0x3F));
	}

	/** Encode the given integer (from 0 to 63) into its base 64 character. */
	private byte encodeSafe(int v) {
		if (v <= 25) return (byte)(v + 'A');
		if (v <= 51) return (byte)(v - 26 + 'a');
		if (v <= 61) return (byte)(v - 52 + '0');
		if (v == 62) return char62;
		if (v == 63) return char63;
		return 0;
	}

	/** Encode the given integer (from 0 to 63) into its base 64 character. */
	public byte encode(int v) throws EncodingException {
		if (v < 0)
			throw new InvalidBase64Value(v);
		byte b = encodeSafe(v);
		if (b == 0)
			throw new InvalidBase64Value(v);
		return b;
	}
	
	@Override
	public byte[] encode(byte[] input) {
		return encode(input, 0, input.length);
	}
	
	/** Encode the given bytes into base 64. */
	@Override
	public byte[] encode(byte[] input, int offset, int len) {
		int nb = len / 3;
		if ((len % 3) > 0) nb++;
		nb *= 4;
		byte[] output = new byte[nb];
		int pos = 0;
		while (len >= 3) {
			encode3Bytes(input, offset, output, pos);
			offset += 3;
			len -= 3;
			pos += 4;
		}
		if (len > 0)
			encodeUpTo3Bytes(input, offset, output, pos, len);
		return output;
	}
	
	@Override
	public byte[] encode(Bytes.Readable input) {
		int len = input.remaining();
		int nb = len / 3;
		if ((len % 3) > 0) nb++;
		nb *= 4;
		byte[] output = new byte[nb];
		int pos = 0;
		while (len >= 3) {
			encode3Bytes(input, output, pos);
			len -= 3;
			pos += 4;
		}
		if (len > 0)
			encodeUpTo3Bytes(input, output, pos);
		return output;
	}
	
	@Override
	public int encode(byte[] input, int offset, int length, Bytes.Writable output, boolean end) {
		RawByteBuffer in = new RawByteBuffer(input, offset, length);
		encode(in, output, end);
		return in.position();
	}
	
	@Override
	public void encode(Bytes.Readable input, Bytes.Writable output, boolean end) {
		int l = Math.min(input.remaining() / 3, output.remaining() / 4);
		for (int i = 0; i < l; ++i)
			encode3Bytes(input, output);
		if (end && input.hasRemaining() && output.remaining() >= 4)
			encodeUpTo3Bytes(input, output);
	}

	@Override
	public <TError extends Exception> AsyncConsumer<Bytes.Readable, TError> createEncoderConsumer(
		AsyncConsumer<Bytes.Readable, TError> encodedConsumer, Function<EncodingException, TError> errorConverter
	) {
		return new EncoderConsumer<>(encodedConsumer);
	}

	/** Create an encoder. */
	public <TError extends Exception> AsyncConsumer<Bytes.Readable, TError> createEncoderConsumer(
		AsyncConsumer<Bytes.Readable, TError> encodedConsumer
	) {
		return new EncoderConsumer<>(encodedConsumer);
	}
	
	/** Consume data, encode it as base 64, and gives the encoded bytes to another consumer.
	 * @param <TError> type of error
	 */
	public class EncoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {
		
		/** Constructor. */
		public EncoderConsumer(AsyncConsumer<Bytes.Readable, TError> encodedBytesConsumer) {
			this.cache = ByteArrayCache.getInstance();
			this.encodedBytesConsumer = encodedBytesConsumer;
		}
		
		private ByteArrayCache cache;
		private AsyncConsumer<Bytes.Readable, TError> encodedBytesConsumer;
		private byte[] buf = new byte[3];
		private int nbBuf = 0;

		@Override
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			RawByteBuffer output;
			if (nbBuf != 0) {
				do {
					buf[nbBuf++] = data.get();
				} while (nbBuf < 3 && data.hasRemaining());
				if (nbBuf < 3) {
					if (onDataRelease != null)
						onDataRelease.accept(data);
					return new Async<>(true);
				}
				int nb = data.remaining() / 3 + 1;
				output = new RawByteBuffer(cache.get(nb * 4, true));
				encode(buf, 0, 3, output, false);
				nbBuf = 0;
			} else {
				int nb = data.remaining() / 3;
				output = new RawByteBuffer(cache.get(nb * 4, true));
			}
			encode(data, output, false);
			nbBuf = data.remaining();
			if (nbBuf != 0)
				data.get(buf, 0, nbBuf);
			if (onDataRelease != null)
				onDataRelease.accept(data);
			output.flip();
			return encodedBytesConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array));
		}

		@Override
		public IAsync<TError> end() {
			if (nbBuf == 0)
				return encodedBytesConsumer.end();
			byte[] out = new byte[4];
			encodeUpTo3Bytes(buf, 0, out, 0, nbBuf);
			Async<TError> result = new Async<>();
			encodedBytesConsumer.consume(new RawByteBuffer(out), null).onDone(() -> encodedBytesConsumer.end().onDone(result), result);
			return result;
		}

		@Override
		public void error(TError error) {
			encodedBytesConsumer.error(error);
		}
		
	}
		
	/** Consume data, decode it as base 64, and gives the decoded bytes to another consumer.
	 * @param <TError> type of error
	 */
	public class DecoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {
		
		/** Constructor. */
		public DecoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> decodedBytesConsumer,
			Function<EncodingException, TError> errorConverter
		) {
			this.cache = ByteArrayCache.getInstance();
			this.decodedBytesConsumer = decodedBytesConsumer;
			this.errorConverter = errorConverter;
		}
		
		private ByteArrayCache cache;
		private AsyncConsumer<Bytes.Readable, TError> decodedBytesConsumer;
		private Function<EncodingException, TError> errorConverter;
		private byte[] buf = new byte[4];
		private int nbBuf = 0;

		@Override
		@SuppressWarnings("squid:S2259") // not null
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			RawByteBuffer output = null;
			try {
				int nb;
				if (nbBuf != 0) {
					do {
						buf[nbBuf++] = data.get();
					} while (nbBuf < 4 && data.hasRemaining());
					if (nbBuf < 4) {
						if (onDataRelease != null)
							onDataRelease.accept(data);
						return new Async<>(true);
					}
					nb = data.remaining() / 4;
					output = new RawByteBuffer(cache.get((nb + 1) * 3, true));
					decode4Bytes(buf, 0, output);
					nbBuf = 0;
				} else {
					nb = data.remaining() / 4;
					if (nb > 0)
						output = new RawByteBuffer(cache.get(nb * 3, true));
				}
				if (nb > 0)
					decode(data, output, false);
			} catch (EncodingException e) {
				@SuppressWarnings("unchecked")
				TError err = errorConverter != null ? errorConverter.apply(e) : (TError)e;
				decodedBytesConsumer.error(err);
				return new Async<>(err);
			}
			nbBuf = data.remaining();
			if (nbBuf != 0)
				data.get(buf, 0, nbBuf);
			if (onDataRelease != null)
				onDataRelease.accept(data);
			if (output == null)
				return new Async<>(true);
			output.flip();
			return decodedBytesConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array));
		}

		@Override
		public IAsync<TError> end() {
			if (nbBuf != 0) {
				EncodingException e = new UnexpectedEndOfEncodedData();
				@SuppressWarnings("unchecked")
				TError err = errorConverter != null ? errorConverter.apply(e) : (TError)e;
				decodedBytesConsumer.error(err);
				return new Async<>(err);
			}
			return decodedBytesConsumer.end();
		}

		@Override
		public void error(TError error) {
			decodedBytesConsumer.error(error);
		}

	}
	
}
