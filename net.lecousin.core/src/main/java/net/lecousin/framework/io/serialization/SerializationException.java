package net.lecousin.framework.io.serialization;

/** Error during serialization/deserialization. */
public class SerializationException extends Exception {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public SerializationException(String message) {
		super(message);
	}

	/** Constructor. */
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}