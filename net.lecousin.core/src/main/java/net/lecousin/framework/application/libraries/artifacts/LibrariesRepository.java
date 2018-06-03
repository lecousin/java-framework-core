package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;

import net.lecousin.framework.concurrent.synch.AsyncWork;

/**
 * Interface describing a library repository.
 */
public interface LibrariesRepository {

	/** Load a file. */
	AsyncWork<File, Exception> loadFile(
		String groupId, String artifactId, String version, String classifier, String type, byte priority
	);
	
	/** Load a file without multi-threading, for application initialization. */
	File loadFileSync(
		String groupId, String artifactId, String version, String classifier, String type
	);
	
}
