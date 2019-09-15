package net.lecousin.framework.application.libraries.artifacts.maven;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader;
import net.lecousin.framework.collections.TreeWithParent;
import net.lecousin.framework.collections.TreeWithParent.Node;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.log.Logger;

/**
 * Maven POM file loader.
 */
public class MavenPOMLoader implements LibraryDescriptorLoader {

	private static Logger logger = null;
	
	@Override
	public boolean detect(File dir) {
		return new File(dir, "pom.xml").exists();
	}
	
	@Override
	public AsyncWork<? extends LibraryDescriptor, LibraryManagementException> loadProject(File dir, byte priority) {
		URI pomFile = new File(dir, "pom.xml").toURI();
		try {
			return loadPOM(pomFile, false, priority);
		} catch (Exception e) {
			return new AsyncWork<>(null, new MavenPOMException(pomFile, e));
		}
	}

	/** Load a POM file. */
	public synchronized AsyncWork<MavenPOM, LibraryManagementException> loadPOM(URI pomFile, boolean fromRepository, byte priority) {
		if (logger == null)
			logger = LCCore.getApplication().getLoggerFactory().getLogger(MavenPOMLoader.class);
		/*
		try { pomFile = pomFile.getCanonicalFile(); }
		catch (IOException e) {
			return new AsyncWork<>(null, e);
		}*/
		AsyncWork<MavenPOM, LibraryManagementException> result = loadingByLocation.get(pomFile);
		if (result != null)
			return result;
		if (logger.debug()) logger.debug("Loading POM " + pomFile.toString());
		result = new AsyncWork<>();
		AsyncWork<MavenPOM, LibraryManagementException> loadPOM = MavenPOM.load(pomFile, priority, this, fromRepository);
		loadingByLocation.put(pomFile, result);
		AsyncWork<MavenPOM, LibraryManagementException> res = result;
		loadPOM.listenInline(() -> {
			if (loadPOM.hasError()) {
				if (logger.error()) logger.error("Unable to load POM " + pomFile.toString(), loadPOM.getError());
				res.error(loadPOM.getError());
				return;
			}
			MavenPOM pom = loadPOM.getResult();
			if (pom != null) {
				if (logger.debug()) logger.debug(
					"POM loaded: " + pom.getGroupId() + ':' + pom.getArtifactId() + ':' + pom.getVersionString());
				synchronized (MavenPOMLoader.this) {
					Map<String, Map<Version, AsyncWork<MavenPOM, LibraryManagementException>>> group =
						loadingByName.get(pom.getGroupId());
					if (group == null) {
						group = new HashMap<>();
						loadingByName.put(pom.getGroupId(), group);
					}
					Map<Version, AsyncWork<MavenPOM, LibraryManagementException>> artifact =
						group.get(pom.getArtifactId());
					if (artifact == null) {
						artifact = new HashMap<>();
						group.put(pom.getArtifactId(), artifact);
					}
					AsyncWork<MavenPOM, LibraryManagementException> resu = artifact.get(pom.getVersion());
					if (resu == null) artifact.put(pom.getVersion(), res);
				}
			}
			res.unblockSuccess(pom);
		});
		return result;
	}
	
	@Override
	public synchronized AsyncWork<MavenPOM, LibraryManagementException> loadLibrary(
		String groupId, String artifactId, VersionSpecification version,
		byte priority, List<LibrariesRepository> additionalRepositories
	) {
		if (logger == null)
			logger = LCCore.getApplication().getLoggerFactory().getLogger(MavenPOMLoader.class);
		List<MavenRepository> repos = additionalRepositories.isEmpty() ? new ArrayList<>(repositories.size()) : new LinkedList<>();
		repos.addAll(repositories);
		for (LibrariesRepository repo : additionalRepositories)
			if (repo instanceof MavenRepository)
				repos.add((MavenRepository)repo);
		Map<String, Map<Version, AsyncWork<MavenPOM, LibraryManagementException>>> group = loadingByName.get(groupId);
		if (group == null) {
			group = new HashMap<>();
			loadingByName.put(groupId, group);
		}
		Map<Version, AsyncWork<MavenPOM, LibraryManagementException>> artifact = group.get(artifactId);
		if (artifact == null) {
			artifact = new HashMap<>();
			group.put(artifactId, artifact);
		}
		AsyncWork<MavenPOM, LibraryManagementException> r = null;
		Version rv = null;
		for (Map.Entry<Version, AsyncWork<MavenPOM, LibraryManagementException>> e : artifact.entrySet()) {
			if (!version.isMatching(e.getKey())) continue;
			if (r == null || version.compare(rv, e.getKey()) < 0) {
				r = e.getValue();
				rv = e.getKey();
			}
		}
		if (r != null)
			return r;
		AsyncWork<MavenPOM, LibraryManagementException> result = new AsyncWork<>();
		loadFromRepository(groupId, artifactId, version, priority, repos, 0, artifact, result);
		return result;
	}
	
	@SuppressWarnings("squid:S00107") // number of arguments
	private void loadFromRepository(
		String groupId, String artifactId, VersionSpecification version, byte priority,
		List<MavenRepository> repos, int repoIndex, Map<Version, AsyncWork<MavenPOM, LibraryManagementException>> artifact,
		AsyncWork<MavenPOM, LibraryManagementException> result
	) {
		if (repoIndex == repos.size()) {
			result.error(new LibraryManagementException(
				"Artifact not found: " + Artifact.toString(groupId, artifactId, version.toString())));
			return;
		}
		MavenRepository repo = repos.get(repoIndex);
		if (logger.debug()) logger.debug(
			"Search Maven artifact " + groupId + ':' + artifactId + ':' + version.toString() + " in " + repo.toString());
		AsyncWork<List<String>, NoException> getVersions = repo.getAvailableVersions(groupId, artifactId, priority);
		getVersions.listenAsync(new Task.Cpu.FromRunnable("Search artifact", priority, () -> {
			List<String> versions = getVersions.getResult();
			if (versions == null || versions.isEmpty()) {
				if (logger.debug()) logger.debug(
					"No version found for artifact " + groupId + ':' + artifactId + ':' + version.toString()
					+ " in " + repo.toString());
				loadFromRepository(groupId, artifactId, version, priority, repos, repoIndex + 1, artifact, result);
				return;
			}
			String bestVersionString = null;
			Version bestVersion = null;
			for (String s : versions) {
				Version v = new Version(s);
				if (!version.isMatching(v)) {
					//if (logger.debug()) logger.debug(
					//"Version " + s + " is not matching for artifact " + groupId + ':' + artifactId + ':'
					//+ version.toString() + " in " + repo.toString());
					continue;
				}
				if (bestVersion != null && version.compare(bestVersion, v) > 0) {
					//if (logger.debug()) logger.debug(
					//"Version " + s + " is ignored for artifact " + groupId + ':' + artifactId + ':'
					//+ version.toString() + " in " + repo.toString());
					continue;
				}
				bestVersion = v;
				bestVersionString = s;
				//if (logger.debug()) logger.debug(
				//"Version " + s + " is eligible for artifact " + groupId + ':' + artifactId + ':'
				//+ version.toString() + " in " + repo.toString());
			}
			if (bestVersion == null) {
				loadFromRepository(groupId, artifactId, version, priority, repos, repoIndex + 1, artifact, result);
				return;
			}
			if (logger.debug()) logger.debug(
				"Version " + bestVersionString + " found for artifact " + groupId + ':' + artifactId + ':'
				+ version.toString() + " in " + repo.toString());
			artifact.put(bestVersion, result);
			AsyncWork<MavenPOM, LibraryManagementException> load = repo.load(groupId, artifactId, bestVersionString, this, priority);
			load.listenInline(() -> {
				if (load.getResult() != null) {
					if (logger.debug()) logger.debug(
						"Maven artifact " + groupId + ':' + artifactId + ':' + load.getResult().getVersionString()
						+ " found in " + repo.toString());
					result.unblockSuccess(load.getResult());
					return;
				}
				result.error(new LibraryManagementException(
					"Artifact not found: " + Artifact.toString(groupId, artifactId, version.toString())));
			});
		}), true);
	}
	
	/** Add a repository. */
	public void addRepository(MavenRepository repo) {
		repositories.add(repo);
	}
	
	private Map<String, Map<String, Map<Version, AsyncWork<MavenPOM, LibraryManagementException>>>> loadingByName = new HashMap<>();
	private Map<URI, AsyncWork<MavenPOM, LibraryManagementException>> loadingByLocation = new HashMap<>();
	private List<MavenRepository> repositories = new LinkedList<>();
	private List<MavenRepository> knownRepositories = new LinkedList<>();
	
	MavenRepository getRepository(String url, boolean releasesEnabled, boolean snapshotsEnabled) {
		for (MavenRepository repo : repositories)
			if (repo.isSame(url, releasesEnabled, snapshotsEnabled))
				return null;
		for (MavenRepository repo : knownRepositories)
			if (repo.isSame(url, releasesEnabled, snapshotsEnabled))
				return repo;
		if (url.startsWith("file:/")) {
			try {
				MavenLocalRepository local = new MavenLocalRepository(new File(new URI(url)), releasesEnabled, snapshotsEnabled);
				knownRepositories.add(local);
				return local;
			} catch (URISyntaxException e) {
				return null;
			}
		}
		MavenRemoteRepository remote = new MavenRemoteRepository(url, releasesEnabled, snapshotsEnabled);
		knownRepositories.add(remote);
		return remote;
	}
	
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
