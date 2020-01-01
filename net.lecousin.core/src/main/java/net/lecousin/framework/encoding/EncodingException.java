package net.lecousin.framework.encoding;

/** Encoding error. */
public class EncodingException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public EncodingException(String message) {
		super(message);
	}
	
	/** Constructor. */
	public EncodingException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
