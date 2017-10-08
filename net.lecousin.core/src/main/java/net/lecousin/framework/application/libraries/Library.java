package net.lecousin.framework.application.libraries;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.Version;

/** Library loaded. */
public class Library {

	/** Constructor. */
	public Library(Artifact artifact, ApplicationClassLoader classLoader) {
		this.artifact = artifact;
		this.classLoader = classLoader;
	}
	
	private Artifact artifact;
	private ApplicationClassLoader classLoader;
	
	public ApplicationClassLoader getClassLoader() { return classLoader; }
	
	public String getGroupId() { return artifact.groupId; }
	
	public String getArtifactId() { return artifact.artifactId; }
	
	public Version getVersion() { return artifact.version; }
	
	@Override
	public String toString() {
		return artifact.toString();
	}
	
}
