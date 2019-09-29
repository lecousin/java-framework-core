package net.lecousin.core.loaders.maven.tests;

import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;

import net.lecousin.core.loaders.maven.MavenPOM;
import net.lecousin.core.loaders.maven.MavenPOMLoader;
import net.lecousin.core.loaders.maven.MavenRemoteRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor.Dependency;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestRemoteRepository extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void testRemoteRepository() throws Exception {
		MavenRemoteRepository repo = new MavenRemoteRepository("http://repo.maven.apache.org/maven2/", true, false);

		repo.toString();
		Assert.assertTrue(repo.isReleasesEnabled());
		Assert.assertFalse(repo.isSnapshotsEnabled());
		Assert.assertTrue(repo.isSame("http://repo.maven.apache.org/maven2/", true, false));
		Assert.assertFalse(repo.isSame("http://repo.maven.apache.org/maven2/", true, true));
		Assert.assertFalse(repo.isSame("http://repo.maven.apache.org/maven2/", false, false));
		Assert.assertFalse(repo.isSame("http://repo.maven.apache.org/maven3/", true, false));

		List<String> versions = repo.getAvailableVersions("junit", "junit", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertNotNull(versions);
		if (!versions.contains(junit.runner.Version.id()))
			throw new AssertionError("Available versions of junit does not contain " + junit.runner.Version.id() + ": " + versions.toString());

		versions = repo.getAvailableVersions("junit", "doesnotexist", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
		versions = repo.getAvailableVersions("doesnotexist", "doesnotexist", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
		
		MavenPOMLoader pomLoader = new MavenPOMLoader();
		pomLoader.addRepository(repo);
		
		MavenPOM pom = repo.load("junit", "junit", junit.runner.Version.id(), pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertEquals("junit", pom.getGroupId());
		Assert.assertEquals("junit", pom.getArtifactId());
		Assert.assertEquals(junit.runner.Version.id(), pom.getVersionString());
		
		File file = pom.getClasses().blockResult(0);
		Assert.assertNotNull(file);
		ZipFile zip = new ZipFile(file);
		Assert.assertNotNull(zip.getEntry("junit/runner/Version.class"));
		zip.close();
		
		pom = repo.load("net.lecousin", "parent-pom", "0.8", pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertEquals("net.lecousin", pom.getGroupId());
		Assert.assertEquals("parent-pom", pom.getArtifactId());
		Assert.assertEquals("0.8", pom.getVersionString());
		Assert.assertFalse(pom.hasClasses());

		try {
			pom = repo.load("junit", "doesnotexist", junit.runner.Version.id(), pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
			if (pom != null)
				throw new AssertionError("should fail");
		} catch (Exception e) {
			// ok
		}
		
		pom = repo.load("org.apache.pdfbox", "pdfbox", "2.0.9", pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		for (Dependency dep : pom.getDependencies()) {
			dep.getGroupId();
			dep.getArtifactId();
			dep.getClassifier();
			dep.getVersionSpecification();
			dep.getExcludedDependencies();
			dep.getKnownLocation();
			dep.isOptional();
		}
		pom.getDependenciesAdditionalRepositories();
		pom.getLoader();
		pom.getDirectory();
		
		pom = repo.load("com.google.guava", "guava-parent", "22.0", pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		for (Dependency dep : pom.getDependencies()) {
			dep.getGroupId();
			dep.getArtifactId();
			dep.getClassifier();
			dep.getVersionSpecification();
			dep.getExcludedDependencies();
			dep.getKnownLocation();
			dep.isOptional();
		}
		pom.getDependenciesAdditionalRepositories();
		pom.getLoader();
		pom.getDirectory();

		
		pom = repo.load("org.javassist", "javassist", "3.16.1-GA", pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		boolean foundTools = false;
		for (Dependency dep : pom.getAllDependenciesAnyScope()) {
			if ("com.sun".equals(dep.getGroupId()) && "tools".equals(dep.getArtifactId()))
				foundTools = true;
			dep.getClassifier();
			dep.getVersionSpecification();
			dep.getExcludedDependencies();
			dep.getKnownLocation();
			dep.isOptional();
		}
		pom.getDependenciesAdditionalRepositories();
		pom.getLoader();
		pom.getDirectory();
		Assert.assertTrue("dependency com.sun/tools found", foundTools);
		
		pom = repo.load("org.apache.maven.shared", "maven-filtering", "1.3", pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		for (Dependency dep : pom.getAllDependenciesAnyScope()) {
			dep.getGroupId();
			dep.getArtifactId();
			dep.getClassifier();
			dep.getVersionSpecification();
			dep.getExcludedDependencies();
			dep.getKnownLocation();
			dep.isOptional();
		}
		pom.getDependenciesAdditionalRepositories();
		pom.getLoader();
		pom.getDirectory();
		
		repo = new MavenRemoteRepository("http://repo.maven.apache.org/maven2", false, true);
	}
	
}
