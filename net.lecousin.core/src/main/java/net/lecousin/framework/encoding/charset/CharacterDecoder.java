package net.lecousin.framework.encoding.charset;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Decoder from bytes to characters for a specific charset.
 * Important note: the result may be a wrapper on the given bytes, meaning any change on the given bytes
 * after the decoding may affect the decoded characters.
 */
public interface CharacterDecoder {
	
	/** Create an instance for the given charset. */
	static CharacterDecoder get(Charset charset, int bufferSize) {
		Constructor<? extends CharacterDecoder> ctor = Registry.decoders.get(charset.name());
		if (ctor != null) {
			try {
				return ctor.newInstance(Integer.valueOf(bufferSize));
			} catch (Exception e) {
				LCCore.getApplication().getDefaultLogger().error("Unable to instantiate character decoder " + ctor, e);
			}
		}
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		return new CharacterDecoderFromCharsetDecoder(decoder, bufferSize);
	}
	
	/** Decoder registry. */
	static final class Registry {
		private Registry() { /* no instance */ }
		
		/** Register a decoder implementation. 
		 * @throws NoSuchMethodException in case the constructor does not exist
		 */
		public static void register(Charset charset, Class<? extends CharacterDecoder> decoder)
		throws NoSuchMethodException {
			decoders.put(charset.name(), decoder.getConstructor(int.class));
		}
		
		private static Map<String, Constructor<? extends CharacterDecoder>> decoders = new HashMap<>();
	
		static {
			try {
				register(StandardCharsets.UTF_8, UTF8Decoder.class);
				register(StandardCharsets.US_ASCII, UsAsciiDecoder.class);
				register(StandardCharsets.ISO_8859_1, Iso8859Decoder.class);
			} catch (Exception e) {
				// not possible
			}
		}
	}

	/** Return the charset of this decoder. */
	Charset getEncoding();

	/** Decode input.
	 * The full input is decoded, so input.remaining() is always 0 when this method returns.
	 * 
	 * @param input bytes to decode
	 * @return decoded characters
	 */
	Chars.Readable decode(Bytes.Readable input);
	
	/** End of input, generates any remaining characters or return null.
	 * Once called, the decoder can be used again.
	 */
	Chars.Readable flush();
	
	/** Create a consumer of bytes that decodes into characters and give the decoded data to the given consumer. */
	default <TError extends Exception> AsyncConsumer<Bytes.Readable, TError>
	createConsumer(AsyncConsumer<Chars.Readable, TError> charsConsumer) {
		return new AsyncConsumer<Bytes.Readable, TError>() {

			@Override
			public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
				return charsConsumer.consume(decode(data), b -> { if (onDataRelease != null) onDataRelease.accept(data); });
			}

			@Override
			public IAsync<TError> end() {
				Chars.Readable chars = flush();
				if (chars == null)
					return charsConsumer.end();
				Async<TError> result = new Async<>();
				charsConsumer.consume(chars, null).onDone(() -> charsConsumer.end().onDone(result), result);
				return result;
			}

			@Override
			public void error(TError error) {
				charsConsumer.error(error);
			}
			
		};
	}
	
	/** Create a consumer decoding bytes and calling <code>onEnd</code> with decoded string. */
	default <TError extends Exception> AsyncConsumer<Bytes.Readable, TError>
	decodeConsumerToString(Consumer<String> onEnd) {
		return new AsyncConsumer<Bytes.Readable, TError>() {
			
			private UnprotectedStringBuffer str = new UnprotectedStringBuffer();

			@Override
			public IAsync<TError> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
				Chars.Readable chars = decode(data);
				chars.get(str, chars.remaining());
				return new Async<>(true);
			}

			@Override
			public IAsync<TError> end() {
				Chars.Readable chars = flush();
				if (chars != null)
					chars.get(str, chars.remaining());
				onEnd.accept(str.toString());
				return new Async<>(true);
			}

			@Override
			public void error(TError error) {
				// ignore
			}
			
		};
	}
	
}
