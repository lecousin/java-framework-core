package net.lecousin.framework.application.libraries.artifacts.maven;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;

public abstract class MavenRepository {

	public abstract List<String> getAvailableVersions(String groupId, String artifactId);
	
	public abstract AsyncWork<MavenPOM, Exception> load(String groupId, String artifactId, String version, MavenPOMLoader pomLoader, byte priority);
	
}
