package net.lecousin.framework.encoding;

import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.io.data.RawCharBuffer;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Encode and decode quoted-printable as defined in RFC 2045. */
public final class QuotedPrintable {
	
	private QuotedPrintable() { /* no instance. */ }
	
	public static final int MAX_LINE_CHARACTERS = 76;

	/** Invalid hexadecimal value following '='. */
	public static class InvalidQuotedPrintableHexadecimalValue extends EncodingException {
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public InvalidQuotedPrintableHexadecimalValue(char c) {
			super("Invalid hexadecimal value in quoted-printable: " + c);
		}
	}

	/** Invalid byte for quoted-printable encoding. */
	public static class InvalidQuotedPrintableByte extends EncodingException {
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public InvalidQuotedPrintableByte(byte b) {
			super("Invalid byte in quoted-printable: " + (b & 0xFF));
		}
	}
	
	/** Quoted-printable decoder. */
	public static class Decoder implements BytesDecoder {
		
		private byte[] spaces;
		private int nbSpaces;
		private int decodedByte = -1;
		private int hexa = -2;
	
		@Override
		public void decode(Bytes.Readable input, Bytes.Writable output, boolean end) throws EncodingException {
			if (decodedByte != -1) {
				flushSpaces(output);
				if (!output.hasRemaining())
					return;
				output.put((byte)decodedByte);
				decodedByte = -1;
			}
			if (hexa != -2) {
				if (!flushSpaces(output) || !decodeHexa(input, output, end))
					return;
				hexa = -2;
			}
			while (input.hasRemaining() && output.hasRemaining()) {
				byte b = input.get();
				if ((b >= 33 && b <= 60) || (b >= 62 && b <= 126)) {
					if (!flushSpaces(output)) {
						decodedByte = b;
						return;
					}
					output.put(b);
					continue;
				}
				switch (b) {
				case '=':
					if (!flushSpaces(output)) {
						hexa = -1;
						return;
					}
					if (!decodeHexa(input, output, end))
						return;
					continue;
				case ' ': case '\t':
					addSpace(b);
					continue;
				case '\r':
					continue;
				case '\n':
					// end of line
					nbSpaces = 0;
					continue;
				default:
					throw new InvalidQuotedPrintableByte(b);
				}
			}
		}
		
		private boolean decodeHexa(Bytes.Readable input, Bytes.Writable output, boolean end) throws InvalidQuotedPrintableHexadecimalValue {
			char c1;
			char c2;
			if (hexa >= 0) {
				if (!input.hasRemaining()) {
					if (end)
						input.goToEnd();
					return false;
				}
				c1 = (char)hexa;
				c2 = (char)(input.get() & 0xFF);
			} else {
				if (input.remaining() < 2) {
					// not enough data to read
					if (end)
						input.goToEnd();
					else if (input.hasRemaining()) {
						hexa = input.get() & 0xFF;
					} else {
						hexa = -1;
					}
					return false;
				}
				c1 = (char)(input.get() & 0xFF);
				c2 = (char)(input.get() & 0xFF);
			}
			if (c1 == '\r' && c2 == '\n') {
				// soft line break
				nbSpaces = 0;
				return true;
			}
			int i;
			try { i = HexaDecimalEncoding.decodeChar(c1); }
			catch (EncodingException e) { throw new InvalidQuotedPrintableHexadecimalValue(c1); }
			int j;
			try { j = HexaDecimalEncoding.decodeChar(c2); }
			catch (EncodingException e) { throw new InvalidQuotedPrintableHexadecimalValue(c2); }
			output.put((byte)((i << 4) | j));
			return true;
		}
		
		private void addSpace(byte space) {
			if (spaces == null)
				spaces = new byte[16];
			else if (nbSpaces == spaces.length) {
				byte[] s = new byte[spaces.length * 2];
				System.arraycopy(spaces, 0, s, 0, spaces.length);
				spaces = s;
			}
			spaces[nbSpaces++] = space;
		}
		
		private boolean flushSpaces(Bytes.Writable output) {
			if (nbSpaces == 0)
				return output.hasRemaining();
			int pos = 0;
			while (output.hasRemaining() && pos < nbSpaces)
				output.put(spaces[pos++]);
			if (pos == nbSpaces) {
				nbSpaces = 0;
			} else {
				System.arraycopy(spaces, pos, spaces, 0, nbSpaces - pos);
				nbSpaces -= pos;
			}
			return output.hasRemaining();
		}
		
		@Override
		public <TError extends Exception> AsyncConsumer<Bytes.Readable, TError> createDecoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> decodedConsumer, Function<EncodingException, TError> errorConverter
		) {
			return new DecoderConsumer<>(decodedConsumer, ByteArrayCache.getInstance(), 0, errorConverter, this);
		}
	}
	
	/** Consume bytes, decode them, and give the decoded bytes to the next consumer.
	 * @param <TError> type of error
	 */
	public static class DecoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {

		/** Constructor. */
		public DecoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> decodedConsumer, ByteArrayCache cache, int bufferSize,
			Function<EncodingException, TError> errorConverter
		) {
			this(decodedConsumer, cache, bufferSize, errorConverter, new Decoder());
		}

		/** Constructor. */
		public DecoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> decodedConsumer, ByteArrayCache cache, int bufferSize,
			Function<EncodingException, TError> errorConverter, Decoder decoder
		) {
			this.decodedConsumer = decodedConsumer;
			this.cache = cache;
			this.bufferSize = bufferSize;
			this.errorConverter = errorConverter;
			this.decoder = decoder;
		}
		
		private AsyncConsumer<Bytes.Readable, TError> decodedConsumer;
		private ByteArrayCache cache;
		private int bufferSize;
		private Function<EncodingException, TError> errorConverter;
		private Decoder decoder;
		
		@Override
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			Async<TError> result = new Async<>();
			continueDecoding(data, onDataRelease, result);
			return result;
		}
		
		private void continueDecoding(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease, Async<TError> result) {
			RawByteBuffer output = new RawByteBuffer(cache.get(bufferSize > 16 ? bufferSize : Math.max(data.remaining(), 128), true));
			try {
				decoder.decode(data, output, false);
			} catch (EncodingException e) {
				@SuppressWarnings("unchecked")
				TError err = errorConverter != null ? errorConverter.apply(e) : (TError)e;
				decodedConsumer.error(err);
				result.error(err);
				return;
			}
			if (output.hasRemaining() || !data.hasRemaining()) {
				// everything has been consumed
				if (onDataRelease != null)
					onDataRelease.accept(data);
				output.flip();
				decodedConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array)).onDone(result);
				return;
			}
			// we need to call again the decoder
			output.flip();
			decodedConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array))
				.onDone(() -> continueDecoding(data, onDataRelease, result), result);
		}

		@Override
		public IAsync<TError> end() {
			RawByteBuffer output = new RawByteBuffer(cache.get(bufferSize, true));
			try {
				decoder.decode(new RawByteBuffer(new byte[0]), output, true);
			} catch (EncodingException e) {
				@SuppressWarnings("unchecked")
				TError err = errorConverter != null ? errorConverter.apply(e) : (TError)e;
				decodedConsumer.error(err);
				return new Async<>(err);
			}
			return decodedConsumer.end();
		}

		@Override
		public void error(TError error) {
			decodedConsumer.error(error);
		}
		
	}
	
	/** Decode the given bytes into a string. */
	public static UnprotectedStringBuffer decodeAsString(Bytes.Readable input) throws EncodingException {
		UnprotectedStringBuffer str = new UnprotectedStringBuffer();
		Decoder decoder = new Decoder();
		do {
			char[] chars = new char[Math.max(input.remaining(), 128)];
			RawCharBuffer output = new RawCharBuffer(chars);
			decoder.decode(input, output.asWritableAsciiBytes(), true);
			if (output.currentOffset > 0)
				str.append(chars, 0, output.currentOffset);
			else
				break;
		} while (input.hasRemaining());
		return str;
	}
	
	/** Encode bytes into quoted-printable. */
	public static class Encoder implements BytesEncoder {
		
		private int linePos = 0;
		private int softBreak = 0;
		private int toEncode = -1;
		private int encodePos = 0;
		private byte[] spaces = new byte[76];
		private int nbSpaces = 0;
		private int finalSpacePos = 0;
		private int finalSpacesPos = 0;
		
		@Override
		public void encode(Bytes.Readable input, Bytes.Writable output, boolean end) {
			while ((toEncode != -1 || input.hasRemaining()) && output.hasRemaining()) {
				if (!encodeLoop(input, output))
					return;
			}
			if (!input.hasRemaining() && output.hasRemaining() && end && nbSpaces > finalSpacesPos) {
				// all remaining spaces need to be encoded
				finalSpaces(output);
			}
		}
		
		private boolean encodeLoop(Bytes.Readable input, Bytes.Writable output) {
			if (softBreak != 0) {
				addSoftBreak(output);
				return true;
			}
			if (linePos + nbSpaces >= MAX_LINE_CHARACTERS - 1)
				return flushSpacesEndOfLines(output);
			byte b;
			if (toEncode != -1) {
				if (encodePos > 0) {
					continueEncodeByte(output);
					return true;
				}
				b = (byte)toEncode;
				toEncode = -1;
			} else {
				b = input.get();
				if (b == ' ' || b == '\t') {
					// space (cannot be in toEncode)
					return addSpace(b, output);
				}
			}
			if (!flushSpaces(output)) {
				toEncode = b;
				return false;
			}
			if ((b >= 33 && b <= 60) || (b >= 62 && b <= 126)) {
				// no need to encode
				output.put(b);
				linePos++;
				if (linePos == MAX_LINE_CHARACTERS - 1) {
					softBreak = 1;
					linePos = 0;
				}
				return true;
			}
			// need to encode
			if (linePos > MAX_LINE_CHARACTERS - 4) {
				toEncode = b & 0xFF;
				linePos = 0;
				softBreak = 1;
				return true;
			}
			return encodeByte(b, output);
		}
		
		private void addSoftBreak(Bytes.Writable output) {
			do {
				switch (softBreak) {
				case 1:
					output.put((byte)'=');
					softBreak++;
					continue;
				case 2:
					output.put((byte)'\r');
					softBreak++;
					continue;
				case 3:
					output.put((byte)'\n');
					softBreak = 0;
					continue;
				default:
					return;
				}
			} while (output.hasRemaining());
		}
		
		private boolean encodeByte(byte b, Bytes.Writable output) {
			output.put((byte)'=');
			linePos++;
			if (output.remaining() >= 2) {
				output.put((byte)HexaDecimalEncoding.encodeDigit((b & 0xF0) >> 4));
				output.put((byte)HexaDecimalEncoding.encodeDigit(b & 0xF));
				linePos += 2;
				return true;
			}
			if (output.hasRemaining()) {
				output.put((byte)HexaDecimalEncoding.encodeDigit((b & 0xF0) >> 4));
				linePos++;
				encodePos = 2;
				toEncode = b & 0xFF;
				return false;
			}
			encodePos = 1;
			toEncode = b & 0xFF;
			return false;
		}
		
		private void continueEncodeByte(Bytes.Writable output) {
			if (encodePos == 1) {
				output.put((byte)HexaDecimalEncoding.encodeDigit((toEncode & 0xF0) >> 4));
				linePos++;
				encodePos++;
				if (!output.hasRemaining())
					return;
			}
			output.put((byte)HexaDecimalEncoding.encodeDigit(toEncode & 0xF));
			linePos++;
			encodePos = 0;
			toEncode = -1;
		}
		
		private boolean addSpace(byte b, Bytes.Writable output) {
			spaces[nbSpaces++] = b;
			if (linePos + nbSpaces >= MAX_LINE_CHARACTERS - 1)
				return flushSpacesEndOfLines(output);
			return true;
		}
		
		private boolean flushSpacesEndOfLines(Bytes.Writable output) {
			int l = (MAX_LINE_CHARACTERS - 1) - linePos;
			l = Math.min(l, output.remaining());
			output.put(spaces, 0, l);
			nbSpaces -= l;
			if (nbSpaces > 0)
				System.arraycopy(spaces, l, spaces, 0, nbSpaces);
			linePos += l;
			if (linePos == MAX_LINE_CHARACTERS - 1) {
				softBreak = 1;
				linePos = 0;
			}
			return output.hasRemaining();
		}
		
		private boolean flushSpaces(Bytes.Writable output) {
			if (nbSpaces == 0)
				return true;
			int l = Math.min(nbSpaces, output.remaining());
			output.put(spaces, 0, l);
			nbSpaces -= l;
			if (nbSpaces > 0)
				System.arraycopy(spaces, l, spaces, 0, nbSpaces);
			linePos += l;
			return output.hasRemaining();
		}
		
		private void finalSpaces(Bytes.Writable output) {
			do {
				if (softBreak != 0) {
					addSoftBreak(output);
					continue;
				}
				if (linePos > MAX_LINE_CHARACTERS - 4) {
					linePos = 0;
					softBreak = 1;
					continue;
				}
				switch (finalSpacePos) {
				case 0:
					output.put((byte)'=');
					finalSpacePos++;
					continue;
				case 1:
					output.put((byte)'0');
					finalSpacePos++;
					continue;
				default:
					output.put((byte)HexaDecimalEncoding.encodeDigit((spaces[finalSpacesPos++]) & 0xF));
					if (finalSpacesPos == nbSpaces)
						return;
				}
			} while (output.hasRemaining());
		}
		
		@Override
		public <TError extends Exception> AsyncConsumer<Bytes.Readable, TError> createEncoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> encodedConsumer, Function<EncodingException, TError> errorConverter
		) {
			return new EncoderConsumer<>(encodedConsumer, ByteArrayCache.getInstance(), 0, this);
		}
	}
	
	/** Consume bytes, encode them, and give the encoded bytes to the next consumer.
	 * @param <TError> type of error
	 */
	public static class EncoderConsumer<TError extends Exception> implements AsyncConsumer<Bytes.Readable, TError> {
		
		/** Constructor. */
		public EncoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> encodedConsumer, ByteArrayCache cache, int bufferSize
		) {
			this(encodedConsumer, cache, bufferSize, new Encoder());
		}
		
		/** Constructor. */
		public EncoderConsumer(
			AsyncConsumer<Bytes.Readable, TError> encodedConsumer, ByteArrayCache cache, int bufferSize, Encoder encoder
		) {
			this.encodedConsumer = encodedConsumer;
			this.cache = cache;
			this.bufferSize = bufferSize;
			this.encoder = encoder;
		}
		
		private AsyncConsumer<Bytes.Readable, TError> encodedConsumer;
		private ByteArrayCache cache;
		private int bufferSize;
		private Encoder encoder;
		
		@Override
		public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
			Async<TError> result = new Async<>();
			continueEncoding(data, onDataRelease, result);
			return result;
		}
		
		private void continueEncoding(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease, Async<TError> result) {
			RawByteBuffer output = new RawByteBuffer(cache.get(bufferSize > 16 ? bufferSize : data.remaining() + 32, true));
			encoder.encode(data, output, false);
			if (output.hasRemaining() || !data.hasRemaining()) {
				// everything has been consumed
				if (onDataRelease != null)
					onDataRelease.accept(data);
				output.flip();
				encodedConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array)).onDone(result);
				return;
			}
			// we need to call again the encoder
			output.flip();
			encodedConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array))
				.thenStart("Encoding quoted-printable data", Task.PRIORITY_NORMAL,
					() -> continueEncoding(data, onDataRelease, result), result);
		}

		@Override
		public IAsync<TError> end() {
			RawByteBuffer output = new RawByteBuffer(cache.get(32, true));
			encoder.encode(new RawByteBuffer(new byte[0]), output, true);
			if (output.currentOffset > 0) {
				Async<TError> result = new Async<>();
				output.flip();
				encodedConsumer.consume(output, b -> cache.free(((RawByteBuffer)b).array))
				.onDone(() -> encodedConsumer.end().onDone(result), result);
				return result;
			}
			return encodedConsumer.end();
		}

		@Override
		public void error(TError error) {
			encodedConsumer.error(error);
		}
		
	}
	
}
