package net.lecousin.framework.application.libraries.artifacts.maven;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;

/**
 * Interface for a maven repository implementation.
 */
public interface MavenRepository {

	/** Return the list of available versions for the given artifact. */
	List<String> getAvailableVersions(String groupId, String artifactId);
	
	/** Load an artifact's POM file. */
	AsyncWork<MavenPOM, Exception> load(String groupId, String artifactId, String version, MavenPOMLoader pomLoader, byte priority);
	
}
