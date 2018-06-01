package net.lecousin.framework.application.libraries.artifacts.maven;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;

/**
 * Implementation of a Maven repository, using a local directory.
 */
public class MavenLocalRepository implements MavenRepository {

	/** Constructor. */
	public MavenLocalRepository(File dir) {
		this.dir = dir;
	}
	
	private File dir;
	
	@Override
	public List<String> getAvailableVersions(String groupId, String artifactId) {
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
	
	@Override
	public AsyncWork<MavenPOM, Exception> load(String groupId, String artifactId, String version, MavenPOMLoader pomLoader, byte priority) {
		AsyncWork<MavenPOM, Exception> result = new AsyncWork<>();
		Task<Void, NoException> task = new Task.OnFile<Void, NoException>(dir, "Search Maven POM in local repository", priority) {
			@Override
			public Void run() {
				File d = new File(dir, groupId.replace('.', '/'));
				if (!d.exists()) {
					result.unblockSuccess(null);
					return null;
				}
				d = new File(d, artifactId);
				if (!d.exists()) {
					result.unblockSuccess(null);
					return null;
				}
				d = new File(d, version);
				if (!d.exists()) {
					result.unblockSuccess(null);
					return null;
				}
				File pom = new File(d, artifactId + '-' + version + ".pom");
				if (!pom.exists()) {
					result.unblockSuccess(null);
					return null;
				}
				pomLoader.loadPOM(pom, priority).listenInline(result);
				return null;
			}
		};
		task.start();
		return result;
	}
	
	
	@Override
	public String toString() {
		return "local maven repository (" + dir.getAbsolutePath() + ")";
	}
}
