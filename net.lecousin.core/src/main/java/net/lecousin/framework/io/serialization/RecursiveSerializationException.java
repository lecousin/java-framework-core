package net.lecousin.framework.io.serialization;

/** Exception raised when a cycle is detected in serialization. */
public class RecursiveSerializationException extends Exception {

	private static final long serialVersionUID = 3289342236650906292L;

	/** Constructor. */
	public RecursiveSerializationException(Object obj, String path) {
		super("Serialization error: object of type " + obj.getClass().getName() + "(" + obj + ") contains itself: " + path);
	}
	
}
