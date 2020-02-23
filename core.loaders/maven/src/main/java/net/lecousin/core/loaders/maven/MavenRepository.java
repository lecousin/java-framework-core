package net.lecousin.core.loaders.maven;

import java.util.List;

import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;

/**
 * Interface for a maven repository implementation.
 */
public interface MavenRepository extends LibrariesRepository {

	/** Return the list of available versions for the given artifact, or null if the artifact does not exist. */
	AsyncSupplier<List<String>, NoException> getAvailableVersions(String groupId, String artifactId, Priority priority);
	
	/** Load an artifact's POM file. */
	AsyncSupplier<MavenPOM, LibraryManagementException> load(
		String groupId, String artifactId, String version, MavenPOMLoader pomLoader, Priority priority
	);
	
	/** Return true if this repository is the same as the given parameters. */
	boolean isSame(String url, boolean releasesEnabled, boolean snapshotsEnabled);
	
	/** Return if releases libraries are enabled on this repository. */
	boolean isReleasesEnabled();
	
	/** Return if snapshots libraries are enabled on this repository. */
	boolean isSnapshotsEnabled();
	
}
