package net.lecousin.framework.util;

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
	
}
