package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.util.Pair;

public interface LibraryDescriptor {

	LibraryDescriptorLoader getLoader();
	
	String getGroupId();
	String getArtifactId();
	Version getVersion();
	default String getVersionString() { return getVersion().toString(); }
	
	File getDirectory();
	boolean hasClasses();
	File getClasses();
	
	public interface Dependency {
		String getGroupId();
		String getArtifactId();
		VersionSpecification getVersionSpecification();
		String getClassifier();
		boolean isOptional();
		File getKnownLocation();
		List<Pair<String, String>> getExcludedDependencies();
	}
	
	List<Dependency> getDependencies();
	
}
