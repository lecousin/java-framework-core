package net.lecousin.framework.application;

/**
 * Reference an artifact by group id and artifact id.
 * The group or artifact id may be set to "*" for any group/artifact.
 */
public class ArtifactReference {

	/** Create a reference to an artifact.
	 * @param groupId group id or *
	 * @param artifactId artifact id or *
	 */
	public ArtifactReference(String groupId, String artifactId) {
		this.groupId = groupId;
		this.artifactId = artifactId;
	}
	
	public String groupId;
	public String artifactId;

	/**
	 * Check the artifact referenced by this instance matches the given group and artifact id.
	 * @param groupId group id
	 * @param artifactId artifact id
	 * @return true if it matches
	 */
	public boolean matches(String groupId, String artifactId) {
		if (!"*".equals(this.groupId) && !this.groupId.equals(groupId))
			return false;
		if (!"*".equals(this.artifactId) && !this.artifactId.equals(artifactId))
			return false;
		return true;
	}
	
}
