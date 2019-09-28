package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.async.AsyncSupplier;

/**
 * Interface describing a library repository.
 */
public interface LibrariesRepository {

	/** Load a file. */
	AsyncSupplier<File, IOException> loadFile(
		String groupId, String artifactId, String version, String classifier, String type, byte priority
	);
	
	/** Load a file without multi-threading, for application initialization. */
	File loadFileSync(
		String groupId, String artifactId, String version, String classifier, String type
	);
	
}
