package net.lecousin.core.loaders.maven.tests;

import java.net.URI;
import java.util.List;

import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.util.Pair;

public class TestDependency implements LibraryDescriptor.Dependency {

	public TestDependency(String groupId, String artifactId, VersionSpecification version, String classifier, boolean optional, URI location, List<Pair<String, String>> excludedDependencies) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.optional = optional;
		this.location = location;
		this.excludedDependencies = excludedDependencies;
	}
	
	private String groupId;
	private String artifactId;
	private VersionSpecification version;
	private String classifier;
	private boolean optional;
	private URI location;
	private List<Pair<String, String>> excludedDependencies;
	
	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public VersionSpecification getVersionSpecification() {
		return version;
	}

	@Override
	public String getClassifier() {
		return classifier;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	@Override
	public URI getKnownLocation() {
		return location;
	}

	@Override
	public List<Pair<String, String>> getExcludedDependencies() {
		return excludedDependencies;
	}

}
