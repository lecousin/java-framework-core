package net.lecousin.core.loaders.maven;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;

/**
 * Implementation of a Maven repository, using a local directory.
 */
public class MavenLocalRepository implements MavenRepository {

	/** Constructor. */
	public MavenLocalRepository(File dir, boolean releasesEnabled, boolean snapshotsEnabled) {
		this.dir = dir;
		this.releasesEnabled = releasesEnabled;
		this.snapshotsEnabled = snapshotsEnabled;
	}
	
	private File dir;
	private boolean releasesEnabled;
	private boolean snapshotsEnabled;
	
	@Override
	@SuppressWarnings("java:S1604")
	public AsyncSupplier<List<String>, NoException> getAvailableVersions(String groupId, String artifactId, Priority priority) {
		return Task.file(dir, "Search artifact versions in local Maven repository", priority, new Executable<List<String>, NoException>() {
			@Override
			public List<String> execute() {
				File d = new File(dir, groupId.replace('.', '/'));
				if (!d.exists()) return null;
				d = new File(d, artifactId);
				if (!d.exists()) return null;
				File[] files = d.listFiles();
				if (files == null) return null;
				List<String> versions = new LinkedList<>();
				for (File f : files) {
					if (!f.isDirectory()) continue;
					File p = new File(f, artifactId + '-' + f.getName() + ".pom");
					if (!p.exists()) continue;
					versions.add(f.getName());
				}
				return versions;
			}
		}).start().getOutput();
	}
	
	@Override
	@SuppressWarnings("java:S1604")
	public AsyncSupplier<MavenPOM, LibraryManagementException> load(
		String groupId, String artifactId, String version, MavenPOMLoader pomLoader, Priority priority
	) {
		if (version.toLowerCase().endsWith("-SNAPSHOT")) {
			if (!snapshotsEnabled)
				return new AsyncSupplier<>(null, null);
		} else {
			if (!releasesEnabled)
				return new AsyncSupplier<>(null, null);
		}
		AsyncSupplier<MavenPOM, LibraryManagementException> result = new AsyncSupplier<>();
		Task.file(dir, "Search Maven POM in local repository", priority, () -> {
			File d = new File(dir, groupId.replace('.', '/'));
			if (d.exists()) {
				d = new File(d, artifactId);
				if (d.exists()) {
					d = new File(d, version);
					if (d.exists()) {
						File pom = new File(d, artifactId + '-' + version + ".pom");
						if (pom.exists()) {
							pomLoader.loadPOM(pom.toURI(), true, priority).forward(result);
							return null;
						}
					}
				}
			}
			result.unblockSuccess(null);
			return null;
		}).start();
		return result;
	}
	
	@Override
	public File loadFileSync(String groupId, String artifactId, String version, String classifier, String type) {
		File d = new File(dir, groupId.replace('.', '/'));
		if (!d.exists()) return null;
		d = new File(d, artifactId);
		if (!d.exists()) return null;
		d = new File(d, version);
		if (!d.exists()) return null;
		File file = new File(d, MavenPOM.getFilename(artifactId, version, classifier, type));
		if (!file.exists()) return null;
		return file;
	}
	
	@Override
	@SuppressWarnings("java:S1604")
	public AsyncSupplier<File, IOException> loadFile(
		String groupId, String artifactId, String version, String classifier, String type, Priority priority
	) {
		return Task.file(dir, "Search file in Maven repository", priority, new Executable<File, IOException>() {
			@Override
			public File execute() {
				return loadFileSync(groupId, artifactId, version, classifier, type);
			}
		}).start().getOutput();
	}
	
	@Override
	public boolean isReleasesEnabled() {
		return releasesEnabled;
	}
	
	@Override
	public boolean isSnapshotsEnabled() {
		return snapshotsEnabled;
	}
	
	@Override
	public boolean isSame(String url, boolean releasesEnabled, boolean snapshotsEnabled) {
		if (releasesEnabled != this.releasesEnabled)
			return false;
		if (snapshotsEnabled != this.snapshotsEnabled)
			return false;
		try {
			URI uri = new URI(url);
			if (!"file".equals(uri.getScheme()))
				return false;
			if (!new File(uri).equals(dir))
				return false;
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "local maven repository (" + dir.getAbsolutePath() + ")";
	}
}
