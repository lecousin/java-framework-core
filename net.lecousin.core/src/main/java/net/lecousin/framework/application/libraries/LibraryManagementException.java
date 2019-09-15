package net.lecousin.framework.application.libraries;

/** Error with libraries. */
public class LibraryManagementException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public LibraryManagementException(String message) {
		super(message);
	}

	/** Constructor. */
	public LibraryManagementException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
