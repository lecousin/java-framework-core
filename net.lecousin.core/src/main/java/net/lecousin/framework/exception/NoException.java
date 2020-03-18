package net.lecousin.framework.exception;

/**
 * Used with classes parameterized with an exception, to declare that no exception can occur.
 */
public class NoException extends Exception {

	private static final long serialVersionUID = 2904591744483153761L;
	
	private NoException() {
		// not instantiable
	}

}
