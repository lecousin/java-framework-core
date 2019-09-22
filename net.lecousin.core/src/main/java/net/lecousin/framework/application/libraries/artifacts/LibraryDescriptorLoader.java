package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.collections.Tree;
import net.lecousin.framework.concurrent.synch.AsyncWork;

/**
 * Loader of library descriptors.
 */
public interface LibraryDescriptorLoader {

	/** Return true if the given directory contains a library descriptor. */
	boolean detect(File dir);
	
	/** Load a library descriptor from the given directory. */
	AsyncWork<? extends LibraryDescriptor, LibraryManagementException> loadProject(File dir, byte priority);
	
	/** Search and load the library descriptor for the given group id, artifact id and version specification. */
	AsyncWork<? extends LibraryDescriptor, LibraryManagementException> loadLibrary(
		String groupId, String artifactId, VersionSpecification version,
		byte priority, List<LibrariesRepository> additionalRepositories
	);
	
	/** Tree node to build a dependency tree. */
	public static class DependencyNode {
		
		/** Constructor. */
		public DependencyNode(LibraryDescriptor.Dependency dep) {
			this.dep = dep;
		}
		
		/** Dependency. */
		private LibraryDescriptor.Dependency dep;
		
		/** Library descriptor. */
		private AsyncWork<? extends LibraryDescriptor, LibraryManagementException> descriptor;
		
		public LibraryDescriptor.Dependency getDependency() {
			return dep;
		}

		public AsyncWork<? extends LibraryDescriptor, LibraryManagementException> getDescriptor() {
			return descriptor;
		}

		public void setDescriptor(AsyncWork<? extends LibraryDescriptor, LibraryManagementException> descr) {
			this.descriptor = descr;
		}
		
	}
	
	/** When several versions of the same library are specified, this method resolve the conflict and return the version to be loaded. */
	Version resolveVersionConflict(String groupId, String artifactId, Map<Version, List<Tree.Node<DependencyNode>>> artifactVersions);
	
}
