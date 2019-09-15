package net.lecousin.framework.application.libraries.artifacts;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.Version;

/** Library loaded. */
public class LoadedLibrary {

	/** Constructor. */
	public <T extends ClassLoader & ApplicationClassLoader> LoadedLibrary(Artifact artifact, T classLoader) {
		this.artifact = artifact;
		this.classLoader = classLoader;
	}
	
	private Artifact artifact;
	private ApplicationClassLoader classLoader;
	
	@SuppressWarnings("unchecked")
	public <T extends ClassLoader & ApplicationClassLoader> T getClassLoader() { return (T)classLoader; }
	
	public String getGroupId() { return artifact.getGroupId(); }
	
	public String getArtifactId() { return artifact.getArtifactId(); }
	
	public Version getVersion() { return artifact.getVersion(); }
	
	@Override
	public String toString() {
		return artifact.toString();
	}
	
}
