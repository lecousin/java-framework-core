package net.lecousin.framework.io.out2in;

import java.io.IOException;

/** Error during the transfer of data. */
public class OutputToInputTransferException extends IOException {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public OutputToInputTransferException(Throwable cause) {
		super("An error occured during the transfer of data", cause);
	}
	
}
