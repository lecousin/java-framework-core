package net.lecousin.framework.util;

import java.nio.ByteBuffer;

import net.lecousin.framework.text.StringUtil;

/** Utility methods for debugging. */
public final class DebugUtil {
	
	private DebugUtil() { /* no instance */ }

	/** Return the String class#method():line of the caller. */
	public static String getCaller() {
		Exception e = new Exception();
		StackTraceElement[] stack = e.getStackTrace();
		if (stack == null || stack.length < 2) return "?";
		// 0 is here, 1 is the caller of this method, 2 is its caller
		return stack[2].getClassName() + "#" + stack[2].getMethodName() + ":" + stack[2].getLineNumber();
	}
	
	/** Append a stack trace of the given Throwable to the StringBuilder. */
	public static StringBuilder createStackTrace(StringBuilder s, Throwable t, boolean includeCause) {
		s.append(t.getClass().getName());
		s.append(": ");
		s.append(t.getMessage());
		createStackTrace(s, t.getStackTrace());
		if (includeCause) {
			Throwable cause = t.getCause();
			if (cause != null && cause != t) {
				s.append("\nCaused by: ");
				createStackTrace(s, cause, true);
			}
		}
		return s;
	}
	
	/** Append a stack trace to a StringBuilder. */
	public static StringBuilder createStackTrace(StringBuilder s, StackTraceElement[] stack) {
		if (stack != null)
			for (StackTraceElement e : stack) {
				s.append("\n\tat ");
				s.append(e.getClassName());
				if (e.getMethodName() != null) {
					s.append('.');
					s.append(e.getMethodName());
				}
				if (e.getFileName() != null) {
					s.append('(');
					s.append(e.getFileName());
					s.append(':');
					s.append(e.getLineNumber());
					s.append(')');
				}
			}
		return s;
	}

	/** Create an hexadecimal dump of the given buffer. */
	public static void dumpHex(StringBuilder s, byte[] buffer, int offset, int length) {
		dumpHex(s, ByteBuffer.wrap(buffer, offset, length));
	}
	
	/** Create an hexadecimal dump of the given buffer. */
	public static void dumpHex(StringBuilder s, ByteBuffer buffer) {
		int savePos = buffer.position();
		int line = 0;
		while (buffer.hasRemaining()) {
			s.append(StringUtil.encodeHexaPadding(16L * line)).append(' ');
			int l = buffer.remaining() >= 16 ? 16 : buffer.remaining();
			byte[] b = new byte[l];
			buffer.get(b);
			for (int i = 0; i < 16; ++i) {
				if (b.length <= i) s.append("   ");
				else s.append(StringUtil.encodeHexa(b[i])).append(' ');
			}
			s.append("  ");
			for (int i = 0; i < 16; ++i) {
				if (b.length <= i) s.append(' ');
				else {
					char c = (char)(b[i] & 0xFF);
					if (c < 0x20 || c > 0x7F) c = '.';
					s.append(c);
				}
			}
			s.append("\r\n");
			line++;
		}
		buffer.position(savePos);
	}	
}
