package net.lecousin.core.loaders.maven;

import java.net.URI;

import net.lecousin.framework.application.libraries.LibraryManagementException;

/** Error loading POM file. */
public class MavenPOMException extends LibraryManagementException {
	
	private static final long serialVersionUID = 1L;
	
	private static final String DEFAULT_MESSAGE = "Error loading POM file ";

	/** Constructor. */
	public MavenPOMException(URI pomFile) {
		super(DEFAULT_MESSAGE + pomFile.toString());
	}

	/** Constructor. */
	public MavenPOMException(URI pomFile, Throwable cause) {
		super(DEFAULT_MESSAGE + pomFile.toString(), cause);
	}

	/** Constructor. */
	public MavenPOMException(URI pomFile, String message) {
		super(DEFAULT_MESSAGE + pomFile.toString() + ": " + message);
	}

	/** Constructor. */
	public MavenPOMException(String message, Throwable cause) {
		super(message, cause);
	}

}
