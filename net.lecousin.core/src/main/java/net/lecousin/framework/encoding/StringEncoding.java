package net.lecousin.framework.encoding;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.io.util.DataUtil;

/** Interface to encode a value into a string, and decode it from a string.
 * @param <T> type to be encoded/decoded
 */
public interface StringEncoding<T> {

	/** Encode into a string. */
	String encode(T value) throws EncodingException;
	
	/** Decode from a string. */
	T decode(String string) throws EncodingException;
	
	/** Simple encoding for an Integer, using toString and valueOf. */
	public static class SimpleInteger implements StringEncoding<Integer> {
		
		@Override
		public String encode(Integer value) {
			return value.toString();
		}
		
		@Override
		public Integer decode(String string) throws EncodingException {
			try {
				return Integer.valueOf(string);
			} catch (NumberFormatException e) {
				throw new EncodingException("Invalid integer: " + string);
			}
		}
		
	}
	
	/** Simple encoding for a Long, using toString and valueOf. */
	public static class SimpleLong implements StringEncoding<Long> {
		
		@Override
		public String encode(Long value) {
			return value.toString();
		}
		
		@Override
		public Long decode(String string) throws EncodingException {
			try {
				return Long.valueOf(string);
			} catch (NumberFormatException e) {
				throw new EncodingException("Invalid long: " + string);
			}
		}
		
	}
	
	/** Uses a BytesEncoder to encode and decode from the little endian representation of a long. */
	public static class EncodedLong implements StringEncoding<Long> {
		
		/** Constructor. */
		public EncodedLong(BytesEncoder.KnownOutputSize encoder, BytesDecoder.KnownOutputSize decoder) {
			this.encoder = encoder;
			this.decoder = decoder;
		}
		
		protected BytesEncoder.KnownOutputSize encoder;
		protected BytesDecoder.KnownOutputSize decoder;
		
		@Override
		public String encode(Long value) throws EncodingException {
			return new String(encoder.encode(DataUtil.getBytesLittleEndian(value.longValue())), StandardCharsets.ISO_8859_1);
		}
		
		@Override
		public Long decode(String string) throws EncodingException {
			return Long.valueOf(DataUtil.readLongLittleEndian(decoder.decode(string.getBytes(StandardCharsets.ISO_8859_1)), 0));
		}
		
	}
	
}
