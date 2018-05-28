package net.lecousin.framework.application.libraries.artifacts;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.collections.TreeWithParent;
import net.lecousin.framework.concurrent.synch.AsyncWork;

public interface LibraryDescriptorLoader {

	boolean detect(File dir);
	
	AsyncWork<? extends LibraryDescriptor, Exception> loadProject(File dir, byte priority);
	AsyncWork<? extends LibraryDescriptor, Exception> loadLibrary(String groupId, String artifactId, VersionSpecification version, byte priority);
	
	public static class DependencyNode {
		public LibraryDescriptor.Dependency dep;
		public AsyncWork<? extends LibraryDescriptor, Exception> descr; 
	}
	
	Version resolveVersionConflict(String groupId, String artifactId, Map<Version, List<TreeWithParent.Node<DependencyNode>>> artifactVersions);
	
}
