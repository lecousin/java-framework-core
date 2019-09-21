package net.lecousin.framework.log;

/** Error in logging configuration. */
public class LoggerConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public LoggerConfigurationException(String message) {
		super(message);
	}

	/** Constructor. */
	public LoggerConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
