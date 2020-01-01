package net.lecousin.framework.encoding;

/** End of data reached unexpectedly. */
public class UnexpectedEndOfEncodedData extends EncodingException {
	
	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public UnexpectedEndOfEncodedData() {
		super("Unexpected end of encoded data");
	}

}
