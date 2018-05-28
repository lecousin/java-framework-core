package net.lecousin.framework.application.libraries.artifacts.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.collections.TreeWithParent;
import net.lecousin.framework.collections.TreeWithParent.Node;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.log.Logger;

public class MavenPOMLoader implements LibraryDescriptorLoader {

	private static Logger logger = null;
	
	@Override
	public boolean detect(File dir) {
		return new File(dir, "pom.xml").exists();
	}
	
	@Override
	public AsyncWork<? extends LibraryDescriptor, Exception> loadProject(File dir, byte priority) {
		return loadPOM(new File(dir, "pom.xml"), priority);
	}

	public synchronized AsyncWork<MavenPOM, Exception> loadPOM(File pomFile, byte priority) {
		if (logger == null)
			logger = LCCore.getApplication().getLoggerFactory().getLogger(MavenPOMLoader.class);
		/*
		try { pomFile = pomFile.getCanonicalFile(); }
		catch (IOException e) {
			return new AsyncWork<>(null, e);
		}*/
		AsyncWork<MavenPOM, Exception> result = loadingByLocation.get(pomFile);
		if (result != null)
			return result;
		if (logger.debug()) logger.debug("Loading POM " + pomFile.getAbsolutePath());
		result = new AsyncWork<>();
		AsyncWork<MavenPOM, Exception> loadPOM = MavenPOM.load(pomFile, priority, this, false);
		loadingByLocation.put(pomFile, result);
		AsyncWork<MavenPOM, Exception> res = result;
		loadPOM.listenInline(new Runnable() {
			@Override
			public void run() {
				if (loadPOM.hasError()) {
					if (logger.error()) logger.error("Unable to load POM " + pomFile.getAbsolutePath(), loadPOM.getError());
					res.error(loadPOM.getError());
					return;
				}
				MavenPOM pom = loadPOM.getResult();
				if (pom != null) {
					if (logger.debug()) logger.debug("POM loaded: " + pom.getGroupId() + ':' + pom.getArtifactId() + ':' + pom.getVersionString());
					synchronized (MavenPOMLoader.this) {
						Map<String, Map<Version, AsyncWork<MavenPOM, Exception>>> group = loadingByName.get(pom.getGroupId());
						if (group == null) {
							group = new HashMap<>();
							loadingByName.put(pom.getGroupId(), group);
						}
						Map<Version, AsyncWork<MavenPOM, Exception>> artifact = group.get(pom.getArtifactId());
						if (artifact == null) {
							artifact = new HashMap<>();
							group.put(pom.getArtifactId(), artifact);
						}
						AsyncWork<MavenPOM, Exception> resu = artifact.get(pom.getVersion());
						if (resu == null) artifact.put(pom.getVersion(), res);
					}
				}
				res.unblockSuccess(pom);
			}
		});
		return result;
	}
	
	@Override
	public synchronized AsyncWork<MavenPOM, Exception> loadLibrary(String groupId, String artifactId, VersionSpecification version, byte priority) {
		if (logger == null)
			logger = LCCore.getApplication().getLoggerFactory().getLogger(MavenPOMLoader.class);
		ArrayList<MavenRepository> repos = new ArrayList<>(repositories.size());
		repos.addAll(repositories);
		Map<String, Map<Version, AsyncWork<MavenPOM, Exception>>> group = loadingByName.get(groupId);
		if (group == null) {
			group = new HashMap<>();
			loadingByName.put(groupId, group);
		}
		Map<Version, AsyncWork<MavenPOM, Exception>> artifact = group.get(artifactId);
		if (artifact == null) {
			artifact = new HashMap<>();
			group.put(artifactId, artifact);
		}
		AsyncWork<MavenPOM, Exception> r = null;
		Version rv = null;
		for (Map.Entry<Version, AsyncWork<MavenPOM, Exception>> e : artifact.entrySet()) {
			if (!version.isMatching(e.getKey())) continue;
			if (r == null || version.compare(rv, e.getKey()) < 0) {
				r = e.getValue();
				rv = e.getKey();
			}
		}
		if (r != null)
			return r;
		AsyncWork<MavenPOM, Exception> result = new AsyncWork<>();
		for (MavenRepository repo : repos) {
			if (logger.debug()) logger.debug("Search Maven artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
			List<String> versions = repo.getAvailableVersions(groupId, artifactId);
			if (versions == null || versions.isEmpty()) {
				if (logger.debug()) logger.debug("No version found for artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
				continue;
			}
			String bestVersionString = null;
			Version bestVersion = null;
			for (String s : versions) {
				Version v = new Version(s);
				if (!version.isMatching(v)) {
					//if (logger.debug()) logger.debug("Version " + s + " is not matching for artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
					continue;
				}
				if (bestVersion != null && version.compare(bestVersion, v) > 0) {
					//if (logger.debug()) logger.debug("Version " + s + " is ignored for artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
					continue;
				}
				bestVersion = v;
				bestVersionString = s;
				//if (logger.debug()) logger.debug("Version " + s + " is eligible for artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
			}
			if (bestVersion == null) continue;
			if (logger.debug()) logger.debug("Version " + bestVersionString + " found for artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
			artifact.put(bestVersion, result);
			AsyncWork<MavenPOM, Exception> load = repo.load(groupId, artifactId, bestVersionString, this, priority);
			load.listenInline(new Runnable() {
				@Override
				public void run() {
					if (load.getResult() != null) {
						if (logger.debug()) logger.debug("Maven artifact " + groupId + ':' + artifactId + ':' + load.getResult().getVersionString() + " found in " + repo.toString());
						result.unblockSuccess(load.getResult());
						return;
					}
					result.error(new Exception("Artifact not found: " + Artifact.toString(groupId, artifactId, version.toString())));
				}
			});
			return result;
		}
		result.error(new Exception("Artifact not found: " + Artifact.toString(groupId, artifactId, version.toString())));
		return result;
	}
	
	public void addRepository(MavenRepository repo) {
		repositories.add(repo);
	}
	
	private Map<String, Map<String, Map<Version, AsyncWork<MavenPOM, Exception>>>> loadingByName = new HashMap<>();
	private Map<File, AsyncWork<MavenPOM, Exception>> loadingByLocation = new HashMap<>();
	private List<MavenRepository> repositories = new LinkedList<>();
	
	@Override
	public Version resolveVersionConflict(String groupId, String artifactId, Map<Version, List<Node<DependencyNode>>> artifactVersions) {
		// maven resolution is
		//  1- the nearest of the root (lower depth)
		//  2- the order of declaration
		Version bestVersion = null;
		int bestDepth = -1;
		for (Map.Entry<Version, List<Node<DependencyNode>>> e : artifactVersions.entrySet()) {
			for (Node<DependencyNode> node : e.getValue()) {
				int depth = 0;
				TreeWithParent<DependencyNode> tree = node.getSubNodes().getParent();
				while (tree.getParent() != null) {
					depth++;
					tree = tree.getParent();
				}
				if (bestVersion == null || depth < bestDepth) {
					bestVersion = e.getKey();
					bestDepth = depth;
				}
			}
		}
		return bestVersion;
	}
	
}
