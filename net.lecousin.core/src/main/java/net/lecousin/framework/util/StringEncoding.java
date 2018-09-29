package net.lecousin.framework.util;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.io.encoding.IBytesEncoding;
import net.lecousin.framework.io.util.DataUtil;

/** Interface to encode a value into a string, and decode it from a string.
 * @param <T> type to be encoded/decoded
 */
public interface StringEncoding<T> {

	/** Encode into a string. */
	String encode(T value);
	
	/** Decode from a string. */
	T decode(String string);
	
	/** Simple encoding for an Integer, using toString and valueOf. */
	public static class SimpleInteger implements StringEncoding<Integer> {
		
		@Override
		public String encode(Integer value) {
			return value.toString();
		}
		
		@Override
		public Integer decode(String string) {
			return Integer.valueOf(string);
		}
		
	}
	
	/** Simple encoding for a Long, using toString and valueOf. */
	public static class SimpleLong implements StringEncoding<Long> {
		
		@Override
		public String encode(Long value) {
			return value.toString();
		}
		
		@Override
		public Long decode(String string) {
			return Long.valueOf(string);
		}
		
	}
	
	/** Uses a IBytesEncoding to encode and decode from the little endian representation of a long. */
	public static class EncodedLong implements StringEncoding<Long> {
		
		/** Constructor. */
		public EncodedLong(IBytesEncoding encoder) {
			this.encoder = encoder;
		}
		
		protected IBytesEncoding encoder;
		
		@Override
		public String encode(Long value) {
			return new String(encoder.encode(DataUtil.getBytesLittleEndian(value.longValue())), StandardCharsets.ISO_8859_1);
		}
		
		@Override
		public Long decode(String string) {
			return Long.valueOf(DataUtil.readLongLittleEndian(encoder.decode(string.getBytes(StandardCharsets.ISO_8859_1)), 0));
		}
		
	}
	
}
