package net.lecousin.framework.application.libraries.artifacts;

import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Libraries manager using artifacts.
 */
public interface ArtifactsLibrariesManager extends LibrariesManager {

	/** Load a new library. */
	AsyncSupplier<LoadedLibrary, LibraryManagementException> loadNewLibrary(
		String groupId, String artifactId, VersionSpecification version, boolean optional,
		Priority priority, WorkProgress progress, long work);

	/** Return a loaded library, or null if not loaded. */
	LoadedLibrary getLibrary(String groupId, String artifactId);
	
	/** Open a resource from a specific library if possible, or return null if it does not exist. */
	IO.Readable getResource(String groupId, String artifactId, String path, Priority priority);

}
