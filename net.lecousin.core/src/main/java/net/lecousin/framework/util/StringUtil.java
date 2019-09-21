package net.lecousin.framework.util;

import java.math.BigInteger;
import java.text.ParseException;

/**
 * Utility methods for String.
 */
public final class StringUtil {
	
	private StringUtil() { /* no instance */ }

	/** Insert padding character to a StringBuilder. */
	public static StringBuilder paddingLeft(StringBuilder s, int length, char padding) {
		while (s.length() < length) s.insert(0, padding);
		return s;
	}

	/** Add the string and insert padding character to a StringBuilder. */
	public static StringBuilder paddingLeft(StringBuilder to, String s, int length, char padding) {
		for (int i = s.length(); i < length; ++i) to.append(padding);
		to.append(s);
		return to;
	}
	
	/** Convert the long to a string and append spaces up to the given fixed size. */
	public static StringBuilder paddingLeft(StringBuilder s, long value, int fixedSize) {
		String str = Long.toString(value);
		return paddingLeft(s, str, fixedSize, ' ');
	}
	
	/** Append padding character until the StringBuilder has the given length. */
	public static StringBuilder paddingRight(StringBuilder s, int length, char padding) {
		while (s.length() < length) s.append(padding);
		return s;
	}
	
	/** Append padding character until the StringBuilder has the given length. */
	public static StringBuilder paddingRight(StringBuilder to, String s, int nb, char padding) {
		to.append(s);
		for (int i = s.length(); i < nb; ++i) to.append(padding);
		return to;
	}
	
	/** Convert the long to a string and append spaces up to the given fixed size. */
	public static StringBuilder paddingRight(StringBuilder s, long value, int fixedSize) {
		String str = Long.toString(value);
		return paddingRight(s, str, fixedSize, ' ');
	}
	
	private static final char[] hexaChar = new char[] { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };
	
	/** Encode a single hexadecimal digit (given value must be between 0 and 15). */
	public static char encodeHexaDigit(int value) {
		return hexaChar[value];
	}
	
	/** Encode the given bytes into hexadecimal. */
	public static String encodeHexa(byte[] data) {
		return encodeHexa(data, 0, data.length);
	}
	
	/** Encode the given bytes into hexadecimal. */
	public static String encodeHexa(byte[] data, int off, int len) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < len; ++i)
			str.append(hexaChar[(data[i + off] >>> 4) & 0xF]).append(hexaChar[data[i + off] & 0xF]);
		return str.toString();
	}
	
	/** Encode the given byte into hexadecimal. */
	public static String encodeHexa(byte b) {
		return new StringBuilder().append(hexaChar[(b >>> 4) & 0xF]).append(hexaChar[b & 0xF]).toString();
	}
	
	/** Encode a long value into hexadecimal, padding with zeros to create a string of 16 characters. */
	public static String encodeHexaPadding(long value) {
		char[] s = new char[16];
		s[0] = encodeHexaDigit((int)((value >> 60) & 0xF));
		s[1] = encodeHexaDigit((int)((value >> 56) & 0xF));
		s[2] = encodeHexaDigit((int)((value >> 52) & 0xF));
		s[3] = encodeHexaDigit((int)((value >> 48) & 0xF));
		s[4] = encodeHexaDigit((int)((value >> 44) & 0xF));
		s[5] = encodeHexaDigit((int)((value >> 40) & 0xF));
		s[6] = encodeHexaDigit((int)((value >> 36) & 0xF));
		s[7] = encodeHexaDigit((int)((value >> 32) & 0xF));
		s[8] = encodeHexaDigit((int)((value >> 28) & 0xF));
		s[9] = encodeHexaDigit((int)((value >> 24) & 0xF));
		s[10] = encodeHexaDigit((int)((value >> 20) & 0xF));
		s[11] = encodeHexaDigit((int)((value >> 16) & 0xF));
		s[12] = encodeHexaDigit((int)((value >> 12) & 0xF));
		s[13] = encodeHexaDigit((int)((value >> 8) & 0xF));
		s[14] = encodeHexaDigit((int)((value >> 4) & 0xF));
		s[15] = encodeHexaDigit((int)(value & 0xF));
		return new String(s);
	}
	
	/** Decode a string containing hexadecimal digits into an array of bytes. */
	public static byte[] decodeHexa(String s) {
		byte[] data = new byte[s.length() / 2];
		decodeHexa(s, data);
		return data;
	}
	
	/** Decode a string containing hexadecimal digits into the given array of bytes. */
	public static void decodeHexa(String s, byte[] data) {
		for (int i = 0; i < data.length; ++i)
			data[i] = (byte)((decodeHexa(s.charAt(i * 2)) << 4) + decodeHexa(s.charAt(i * 2 + 1)));
	}
	
	/** Return the integer value of the given hexadecimal digit, or -1 if it is not an hexadecimal digit. */
	public static int decodeHexa(char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		return -1;
	}
	
	/** Decode the given hexadecimal string into a byte (the string must contain 1 or 2 hexa digits). */
	public static byte decodeHexaByte(String hexa) {
		int i = decodeHexa(hexa.charAt(0));
		if (hexa.length() > 1) {
			i = (i << 4) | decodeHexa(hexa.charAt(1));
		}
		return (byte)i;
	}
	
	/** Decode the given string containing hexadecimal digits into a long value. */
	public static long decodeHexaLong(String hexa) {
		long l = decodeHexa(hexa.charAt(0));
		for (int i = 1; i < hexa.length(); ++i) {
			l <<= 4;
			l |= decodeHexa(hexa.charAt(i));
		}
		return l;
	}
	  
	public static boolean isHexa(char c) { return decodeHexa(c) != -1; }
	
	/** Create a string containing all enumeration values separated by a comma, for printing or debugging purpose. */
	public static String possibleValues(Class<? extends Enum<?>> e) {
		StringBuilder s = new StringBuilder();
		try {
			Enum<?>[] values = (Enum<?>[])e.getMethod("values").invoke(null);
			for (int i = 0; i < values.length; ++i) {
				if (i > 0) s.append(", ");
				s.append(values[i].name());
			}
		} catch (Exception t) { /* ignore */ }
		return s.toString();
	}
	
	/** Create a string for the given size, expressed in bytes or KB or MB or GB or TB, with 2 decimals. */
	public static String size(long size) {
		if (size < 1024)
			return Long.toString(size);
		if (size < 1024 * 1024)
			return String.format("%.2f KB", Double.valueOf(((double)size) / 1024));
		if (size < 1024L * 1024 * 1024)
			return String.format("%.2f MB", Double.valueOf(((double)size) / (1024 * 1024)));
		if (size < 1024L * 1024 * 1024 * 1024)
			return String.format("%.2f GB", Double.valueOf(((double)size) / (1024 * 1024 * 1024)));
		return String.format("%.2f TB", Double.valueOf(((double)size) / (1024L * 1024 * 1024 * 1024)));
	}
	
	/** Create a string for the given size, expressed in bytes or KB or MB or GB or TB, with 2 decimals. */
	public static String size(Number size) {
		if (size instanceof BigInteger)
			return size((BigInteger)size);
		return size(size.longValue());
	}
	
	/** Create a string for the given size, expressed in bytes or KB or MB or GB or TB, with 2 decimals. */
	public static String size(BigInteger s) {
		if (s.compareTo(new BigInteger(Long.toString(Long.MAX_VALUE))) <= 0)
			return size(s.longValue());
		BigInteger tera = s.divide(new BigInteger(Long.toString(1024L * 1024 * 1024 * 1024)));
		return tera.toString() + " TB";
	}
	
	/** Parse the given string containing a size with a unit (B for bytes, K for KB...). */
	@SuppressWarnings("squid:S3776") // complexity
	public static long parseSize(String s) throws ParseException {
		int len = s.length();
		char c = s.charAt(0);
		if (c < '0' || c > '9')
			throw new ParseException("A size must start with a digit: " + s, 0);
		long value = (long)c - '0';
		int pos = 1;
		// read digits
		while (pos < len) {
			c = s.charAt(pos);
			if (c >= '0' && c <= '9') {
				value = value * 10 + (c - '0');
				pos++;
				continue;
			}
			break;
		}
		if (pos == len) return value;
		// skip spaces
		while (pos < len && s.charAt(pos) == ' ') pos++;
		if (pos == len) return value;
		
		c = s.charAt(pos);
		Double dvalue = null;
		if (c == '.') {
			// decimal
			int i = pos + 1;
			while (i < len && Character.isDigit(s.charAt(i))) i++;
			try {
				dvalue = Double.valueOf(Long.toString(value) + s.substring(pos, i));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid size number: " + s, pos);
			}
			pos = i;
			if (pos == len) return value;
			// skip spaces
			while (pos < len && s.charAt(pos) == ' ') pos++;
			if (pos == len) return value;
		}

		// read unit
		c = s.charAt(pos);
		if (c == 'B')
			return value;
		if (c == 'K')
			return dvalue != null ? (long)(dvalue.doubleValue() * 1024) : value * 1024;
		if (c == 'M')
			return dvalue != null ? (long)(dvalue.doubleValue() * 1024 * 1024) : value * 1024 * 1024;
		if (c == 'G')
			return dvalue != null ? (long)(dvalue.doubleValue() * 1024 * 1024 * 1024) : value * 1024 * 1024 * 1024;
		if (c == 'T')
			return dvalue != null ? (long)(dvalue.doubleValue() * 1024 * 1024 * 1024 * 1024) : value * 1024 * 1024 * 1024 * 1024;
		throw new ParseException("Invalid size unit: " + s, pos);
	}
	
	/** Create a string for the given duration. */
	public static String duration(long ms) {
		StringBuilder s = new StringBuilder(20);
		if (ms > 60 * 60 * 1000) {
			s.append(ms / (60 * 60 * 1000)).append('h');
			ms %= 60 * 60 * 1000;
		}
		if (s.length() > 0 || ms > 60 * 1000) {
			s.append(ms / (60 * 1000)).append('m');
			ms %= 60 * 1000;
		}
		if (s.length() > 0 || ms > 1000) {
			s.append(ms / (1000)).append('s');
			ms %= 1000;
		}
		s.append(ms).append("ms");
		return s.toString();
	}
	
	/** Count the number of occurences. */
	public static int count(String string, String toSearch) {
		int pos = 0;
		int i;
		int count = 0;
		while ((i = string.indexOf(toSearch, pos)) != -1) {
			count++;
			pos = i + toSearch.length();
		}
		return count;
	}
}
