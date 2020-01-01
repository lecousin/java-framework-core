package net.lecousin.framework.encoding;

import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.util.Bytes;
import net.lecousin.framework.io.util.Bytes.Readable;
import net.lecousin.framework.io.util.RawByteBuffer;
import net.lecousin.framework.memory.ByteArrayCache;

/** Encode and decode bytes using hexadecimal digits. */
public final class HexaDecimalEncoding implements BytesEncoder.KnownOutputSize, BytesDecoder.KnownOutputSize {
	
	public static final HexaDecimalEncoding instance = new HexaDecimalEncoding();
	
	private HexaDecimalEncoding() { /* singleton */ }
	
	private static final char[] hexaChar = new char[] { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };
	
	/** Encode a single hexadecimal digit (given value must be between 0 and 15). */
	public static char encodeDigit(int value) {
		return hexaChar[value];
	}
	
	/** Return the integer value of the given hexadecimal digit, or -1 if it is not an hexadecimal digit. */
	public static int decodeChar(char c) throws EncodingException {
		if (c >= 'A') {
			if (c <= 'F') return c - 'A' + 10;
			if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		} else if (c >= '0' && c <= '9') {
			return c - '0';
		}
		throw new EncodingException("Invalid hexadecimal digit '" + c + "'");
	}

	/** Return true if the given character is a valid hexadecimal digit. */
	public static boolean isHexaDigit(char c) {
		if (c >= 'A') {
			if (c <= 'F') return true;
			if (c >= 'a' && c <= 'f') return true;
		} else if (c >= '0' && c <= '9') {
			return true;
		}
		return false;
	}

	@Override
	public byte[] encode(byte[] input, int offset, int length) {
		byte[] out = new byte[length * 2];
		for (int i = 0; i < length; ++i) {
			out[i * 2] = (byte)encodeDigit((input[offset + i] & 0xF0) >> 4);
			out[i * 2 + 1] = (byte)encodeDigit(input[offset + i] & 0xF);
		}
		return out;
	}
	
	@Override
	public void encode(Bytes.Readable input, Bytes.Writable output, boolean end) {
		int len = Math.min(input.remaining(), output.remaining() / 2);
		for (int i = 0; i < len; ++i) {
			byte b = input.get();
			output.put((byte)encodeDigit((b & 0xF0) >> 4));
			output.put((byte)encodeDigit(b & 0xF));
		}
	}
	
	@Override
	public byte[] encode(Bytes.Readable input) throws EncodingException {
		byte[] out = new byte[input.remaining() * 2];
		int i = 0;
		while (input.hasRemaining()) {
			byte b = input.get();
			out[i++] = (byte)encodeDigit((b & 0xF0) >> 4);
			out[i++] = (byte)encodeDigit(b & 0xF);
		}
		return out;
	}
	
	@Override
	public byte[] decode(byte[] input, int offset, int length) throws EncodingException {
		byte[] s = new byte[length / 2];
		for (int i = 0; i < s.length; ++i)
			s[i] = (byte)((decodeChar((char)input[offset + i * 2]) << 4) | decodeChar((char)input[offset + i * 2 + 1]));
		return s;
	}
	
	@Override
	public byte[] decode(Bytes.Readable input) throws EncodingException {
		byte[] s = new byte[input.remaining() / 2];
		for (int i = 0; i < s.length; ++i)
			s[i] = (byte)((decodeChar((char)input.get()) << 4) | decodeChar((char)input.get()));
		return s;
	}
	
	@Override
	public void decode(Bytes.Readable input, Bytes.Writable output, boolean end) throws EncodingException {
		int len = Math.min(input.remaining() / 2, output.remaining());
		for (int i = 0; i < len; ++i) {
			output.put((byte)((decodeChar((char)input.get()) << 4) | decodeChar((char)input.get())));
		}
	}
	
	@Override
	public <TError extends Exception> AsyncConsumer<Readable, TError> createDecoderConsumer(
		AsyncConsumer<Readable, TError> decodedConsumer, Function<EncodingException, TError> errorConverter
	) {
		return new DecoderConsumer<>(ByteArrayCache.getInstance(), decodedConsumer, errorConverter);
	}
	
	@Override
	public <TError extends Exception> AsyncConsumer<Readable, TError> createEncoderConsumer(
		AsyncConsumer<Readable, TError> encodedConsumer, Function<EncodingException, TError> errorConverter
	) {
		return new EncoderConsumer<>(ByteArrayCache.getInstance(), encodedConsumer);
	}
	
	/** Consume data, encode it as hexadecimal, and gives the encoded bytes to another consumer.
	 * @param <TError> type of error
	 */
	public static class EncoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {
		
		/** Constructor. */
		public EncoderConsumer(ByteArrayCache cache, AsyncConsumer<Bytes.Readable, TError> encodedBytesConsumer) {
			this.cache = cache;
			this.encodedBytesConsumer = encodedBytesConsumer;
		}
		
		private ByteArrayCache cache;
		private AsyncConsumer<Bytes.Readable, TError> encodedBytesConsumer;

		@Override
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			RawByteBuffer output = new RawByteBuffer(cache.get(data.remaining() * 2, true));
			instance.encode(data, output, false);
			if (onDataRelease != null)
				onDataRelease.accept(data);
			output.flip();
			return encodedBytesConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array));
		}

		@Override
		public IAsync<TError> end() {
			return encodedBytesConsumer.end();
		}

		@Override
		public void error(TError error) {
			encodedBytesConsumer.error(error);
		}
		
	}
	
	/** Consume data, decode it as hexadecimal, and gives the decoded bytes to another consumer.
	 * @param <TError> type of error
	 */
	public static class DecoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {
		
		/** Constructor. */
		public DecoderConsumer(
			ByteArrayCache cache, AsyncConsumer<Bytes.Readable, TError> decodedBytesConsumer,
			Function<EncodingException, TError> errorConverter
		) {
			this.cache = cache;
			this.decodedBytesConsumer = decodedBytesConsumer;
			this.errorConverter = errorConverter;
		}
		
		private ByteArrayCache cache;
		private AsyncConsumer<Bytes.Readable, TError> decodedBytesConsumer;
		private Function<EncodingException, TError> errorConverter;
		private int firstChar = -1;

		@Override
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			int len = ((firstChar != -1 ? 1 : 0) + data.remaining()) / 2;
			if (len == 0) {
				if (data.remaining() == 1)
					firstChar = data.get() & 0xFF;
				if (onDataRelease != null)
					onDataRelease.accept(data);
				return new Async<>(true);
			}
			RawByteBuffer output = new RawByteBuffer(cache.get(len, true));
			try {
				if (firstChar != -1) {
					output.put((byte)((decodeChar((char)firstChar) << 4) | decodeChar((char)data.get())));
					firstChar = -1;
				}
				instance.decode(data, output, false);
			} catch (EncodingException e) {
				@SuppressWarnings("unchecked")
				TError err = errorConverter != null ? errorConverter.apply(e) : (TError)e;
				decodedBytesConsumer.error(err);
				return new Async<>(err);
			}
			if (data.hasRemaining())
				firstChar = data.get() & 0xFF;
			if (onDataRelease != null)
				onDataRelease.accept(data);
			output.flip();
			return decodedBytesConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array));
		}

		@Override
		public IAsync<TError> end() {
			return decodedBytesConsumer.end();
		}

		@Override
		public void error(TError error) {
			decodedBytesConsumer.error(error);
		}

	}
	
}
