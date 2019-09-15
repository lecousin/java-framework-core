package net.lecousin.framework.adapter;

/** Error while adapting an object. */
public class AdapterException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public AdapterException(String message) {
		super(message);
	}

	/** Constructor. */
	public AdapterException(String message, Throwable cause) {
		super(message, cause);
	}

}
