package net.lecousin.framework.util;

import net.lecousin.framework.text.StringUtil;

/**
 * Utility methods for GUID.
 */
public final class GUIDUtil {
	
	private GUIDUtil() {
		/* no instance */
	}
	
	/** Create a GUID. */
	public static byte[] toGUID(long p1, int p2, int p3, int p4, long p5) {
		byte[] guid = new byte[16];
		guid[0] = (byte)(p1 & 0xFF);
		guid[1] = (byte)((p1 >> 8) & 0xFF);
		guid[2] = (byte)((p1 >> 16) & 0xFF);
		guid[3] = (byte)((p1 >> 24) & 0xFF);
		guid[4] = (byte)(p2 & 0xFF);
		guid[5] = (byte)((p2 >> 8) & 0xFF);
		guid[6] = (byte)(p3 & 0xFF);
		guid[7] = (byte)((p3 >> 8) & 0xFF);
		guid[8] = (byte)((p4 >> 8) & 0xFF);
		guid[9] = (byte)(p4 & 0xFF);
		guid[10] = (byte)((p5 >> 40) & 0xFF);
		guid[11] = (byte)((p5 >> 32) & 0xFF);
		guid[12] = (byte)((p5 >> 24) & 0xFF);
		guid[13] = (byte)((p5 >> 16) & 0xFF);
		guid[14] = (byte)((p5 >> 8) & 0xFF);
		guid[15] = (byte)(p5 & 0xFF);
		return guid;
	}

	/** Create a String representation of a GUID. */
	public static String toString(byte[] guid) {
		StringBuilder s = new StringBuilder();
		s.append('{');
		hexa(s, guid[3], guid[2], guid[1], guid[0]);
		s.append('-');
		hexa(s, guid[5], guid[4]);
		s.append('-');
		hexa(s, guid[7], guid[6]);
		s.append('-');
		hexa(s, guid[8], guid[9]);
		s.append('-');
		hexa(s, guid[10], guid[11], guid[12], guid[13], guid[14], guid[15]);
		s.append('}');
		return s.toString();
	}
	
	private static void hexa(StringBuilder s, byte... bytes) {
		for (int i = 0; i < bytes.length; ++i)
			s.append(StringUtil.encodeHexa(bytes[i]));
	}
	
}
