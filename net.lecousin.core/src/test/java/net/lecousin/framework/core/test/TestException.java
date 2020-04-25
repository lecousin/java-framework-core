package net.lecousin.framework.core.test;

import java.io.IOException;

/** Simple marker to indicate this is an error for test and should not be logged. */
public interface TestException {

	public static class Runtime extends RuntimeException implements TestException {
		private static final long serialVersionUID = 1L;

		public Runtime(String message) {
			super(message);
		}
		
		public Runtime(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class IO extends IOException implements TestException {
		private static final long serialVersionUID = 1L;

		public IO(String message) {
			super(message);
		}
		
		public IO(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
