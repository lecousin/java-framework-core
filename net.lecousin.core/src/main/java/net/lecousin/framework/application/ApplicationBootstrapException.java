package net.lecousin.framework.application;

/** Error while starting application. */
public class ApplicationBootstrapException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public ApplicationBootstrapException(String message) {
		super(message);
	}

	/** Constructor. */
	public ApplicationBootstrapException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
