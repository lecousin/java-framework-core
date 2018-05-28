package net.lecousin.framework.application.libraries.artifacts;

import java.util.ArrayList;

import net.lecousin.framework.application.ArtifactReference;
import net.lecousin.framework.application.VersionSpecification;

public class ArtifactDependency {

	public String groupId;
	public String artifactId;
	public VersionSpecification version;
	public boolean optional = false;
	
	public ArrayList<ArtifactReference> exclusions = new ArrayList<>();
	
}
