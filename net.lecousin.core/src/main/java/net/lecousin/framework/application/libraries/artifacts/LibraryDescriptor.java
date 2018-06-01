package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;
import java.util.List;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.util.Pair;

/**
 * Artifact library description.
 */
public interface LibraryDescriptor {

	/** Loader used. */
	LibraryDescriptorLoader getLoader();
	
	/** Artifact group id. */
	String getGroupId();
	
	/** Artifact id. */
	String getArtifactId();
	
	/** Artifact version. */
	Version getVersion();
	
	/** Artifact version as string. */
	default String getVersionString() { return getVersion().toString(); }
	
	/** Return the directory from which the library descriptor as been loaded. */
	File getDirectory();
	
	/** Return true if the artifact contains classes to be loaded. */
	boolean hasClasses();
	
	/** Return a directory or a JAR file containing the classes to be loaded. */
	File getClasses();
	
	
	/** Describes a dependency of a library to another. */
	public interface Dependency {
		/** Dependency group id. */
		String getGroupId();
		
		/** Dependency artifact id. */
		String getArtifactId();
		
		/** Dependency version specification. */
		VersionSpecification getVersionSpecification();
		
		/** Dependency classifier. */
		String getClassifier();
		
		/** Return true if the dependency is optional. */
		boolean isOptional();
		
		/** Return the dependency location if explicitly specified. */
		File getKnownLocation();
		
		/** Return a list of dependencies that should be ignored from this dependency, with pairs of groupId/artifactId. */ 
		List<Pair<String, String>> getExcludedDependencies();
	}
	
	/** Return the list of dependencies of this library. */
	List<Dependency> getDependencies();
	
}
