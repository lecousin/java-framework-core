package net.lecousin.framework.application;

import net.lecousin.framework.application.Version;

/**
 * An artifact is defined by a group id, an artifact id, and a version number.
 * This class inherits from ArtifactReference, and so inherits the two fields groupId and artifactId.
 * An artifact is an artifact reference but wildcards are not allowed.
 */
public class Artifact extends ArtifactReference {

	/** Create an artifact. */
	public Artifact(String groupId, String artifactId, Version version) {
		super(groupId, artifactId);
		this.version = version;
	}
	
	/** Makes a copy of the given artifact. */
	public Artifact(Artifact a) {
		this(a.groupId, a.artifactId, a.version);
	}
	
	public Version version;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Artifact)) return false;
		Artifact a = (Artifact)obj;
		return groupId.equals(a.groupId) && artifactId.equals(a.artifactId) && version.equals(a.version);
	}
	
	@Override
	public int hashCode() {
		return groupId.hashCode() + artifactId.hashCode();
	}
	
	@Override
	public String toString() {
		return toString(groupId, artifactId, version.toString());
	}
	
	/** Create a string with group id, artifact id and version, separated by colon character. */
	public static void toString(StringBuilder s, String groupId, String artifactId, String version) {
		s.append(groupId).append(':').append(artifactId).append(':').append(version);
	}
	
	/** Create a string with group id, artifact id and version, separated by colon character. */
	public static String toString(String groupId, String artifactId, String version) {
		StringBuilder s = new StringBuilder(groupId.length() + artifactId.length() + version.length() + 2);
		toString(s, groupId, artifactId, version);
		return s.toString();
	}
	
}
