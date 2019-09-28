package net.lecousin.framework.concurrent.async;

/** Exception used when cancelling an asynchronous task. */
public class CancelException extends Exception {

	private static final long serialVersionUID = -395406741920805429L;

	/** Constructor. */
	public CancelException(String reason) {
		super(reason);
	}
	
	/** Constructor. */
	public CancelException(String reason, Throwable cause) {
		super(reason, cause);
	}

	/** Constructor. */
	public CancelException(Throwable cause) {
		super(cause.getMessage(), cause);
	}
	
}
