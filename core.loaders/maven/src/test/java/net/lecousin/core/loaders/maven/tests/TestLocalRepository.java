package net.lecousin.core.loaders.maven.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.zip.ZipFile;

import net.lecousin.core.loaders.maven.MavenLocalRepository;
import net.lecousin.core.loaders.maven.MavenPOM;
import net.lecousin.core.loaders.maven.MavenPOMLoader;
import net.lecousin.core.loaders.maven.MavenSettings;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromURI;
import net.lecousin.framework.util.SystemEnvironment;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestLocalRepository extends LCCoreAbstractTest {

	private File repoDir;
	private MavenLocalRepository repo;
	private MavenPOMLoader pomLoader = new MavenPOMLoader();
	
	@Before
	public void initLocalRepo() {
		File settings = new File(System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME) + "/.m2/settings.xml");
		String localRepo = System.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME) + "/.m2/repository";
		if (settings.exists()) {
			try {
				MavenSettings ms = MavenSettings.load(settings);
				if (ms.getLocalRepository() != null)
					localRepo = ms.getLocalRepository();
			} catch (Exception e) {
				System.err.println("Error reading Maven settings.xml");
				e.printStackTrace(System.err);
			}
		}
		repoDir = new File(localRepo);
		repo = new MavenLocalRepository(repoDir, true, true);
		pomLoader.addRepository(repo);
	}
	
	@Test(timeout=120000)
	public void testLocalRepository() throws Exception {
		Assert.assertNotNull(repo.toString());
		Assert.assertTrue(repo.isReleasesEnabled());
		Assert.assertTrue(repo.isSnapshotsEnabled());
		Assert.assertTrue(repo.isSame(repoDir.toURI().toURL().toString(), true, true));
		Assert.assertFalse(repo.isSame(repoDir.toURI().toURL().toString(), true, false));
		Assert.assertFalse(repo.isSame(repoDir.toURI().toURL().toString(), false, true));
		Assert.assertFalse(repo.isSame("file://hello/world", true, true));
		Assert.assertFalse(repo.isSame("http://hello/world", true, true));
		Assert.assertFalse(repo.isSame(repoDir.getParentFile().toURI().toURL().toString(), true, true));
	}
	
	@Test(timeout=120000)
	public void testJUnitVersion() throws Exception {
		List<String> versions = repo.getAvailableVersions("junit", "junit", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertNotNull(versions);
		if (!versions.contains(junit.runner.Version.id()))
			throw new AssertionError("Available versions of junit does not contain " + junit.runner.Version.id() + ": " + versions.toString());
	}
	
	@Test(timeout=120000)
	public void testUnknownArtifactVersion() throws Exception {
		List<String> versions = repo.getAvailableVersions("junit", "doesnotexist", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
		versions = repo.getAvailableVersions("doesnotexist", "doesnotexist", Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
	}
	
	@Test(timeout=120000)
	public void testLoadJUnit() throws Exception {
		MavenPOM pom = repo.load("junit", "junit", junit.runner.Version.id(), pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
		Assert.assertEquals("junit", pom.getGroupId());
		Assert.assertEquals("junit", pom.getArtifactId());
		Assert.assertEquals(junit.runner.Version.id(), pom.getVersionString());
		
		File file = pom.getClasses().blockResult(0);
		Assert.assertNotNull(file);
		ZipFile zip = new ZipFile(file);
		Assert.assertNotNull(zip.getEntry("junit/runner/Version.class"));
		zip.close();
	}

	@Test(timeout=120000)
	public void testLoadUnknownArtifact() throws Exception {
		try {
			MavenPOM pom = repo.load("junit", "doesnotexist", junit.runner.Version.id(), pomLoader, Task.PRIORITY_NORMAL).blockResult(0);
			if (pom != null)
				throw new AssertionError("should fail");
		} catch (Exception e) {
			// ok
		}
		
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("/doesnotexist.pom").toURI(), Task.PRIORITY_NORMAL, pomLoader, false);
		Assert.assertTrue(load.hasError() && load.getError().getCause() instanceof FileNotFoundException);
	}
	
	@Test(timeout=120000)
	public void testDependencyWithSystemPath() throws Exception {
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new URI("classpath:test-maven/test-system-path.pom.xml"), Task.PRIORITY_NORMAL, pomLoader, false);
		MavenPOM pom = load.blockResult(30000);
		List<LibraryDescriptor.Dependency> deps = pom.getAllDependenciesAnyScope();
		Assert.assertFalse(pom.hasClasses());
		Assert.assertNull(pom.getClasses().blockResult(15000));
		Assert.assertEquals(2, deps.size());
		for (LibraryDescriptor.Dependency dep : deps) {
			if (dep.getArtifactId().equals("depsys"))
				Assert.assertEquals("/test", dep.getKnownLocation().toString());
			else if (dep.getArtifactId().equals("depsys2"))
				Assert.assertEquals("file://test", dep.getKnownLocation().toString());
			else
				throw new Exception("Unexpected dependency: " + dep.getArtifactId());
		}
	}
	
	@Test(timeout=120000)
	public void testMyPOM() throws Exception {
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./pom.xml").toURI(), Task.PRIORITY_NORMAL, pomLoader, false);
		MavenPOM pom = load.blockResult(30000);
		Assert.assertEquals(pom.getArtifactId(), "maven");
		
		File classes = pom.getClasses().blockResult(30000);
		Assert.assertNotNull(classes);
		Assert.assertEquals(new File("./target/classes").getAbsolutePath(), classes.getAbsolutePath());
		
		AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException> load2 = pomLoader.loadProject(new File("."), Task.PRIORITY_NORMAL);
		LibraryDescriptor lib = load2.blockResult(30000);
		Assert.assertEquals(lib.getArtifactId(), "maven");
	}
	
	@Test(timeout=120000)
	public void testPOMWithOutputDirectory() throws Exception {
		File outFile = new File("./test-output-dir.pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.PRIORITY_NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/test-output-dir.pom.xml"))).provideIOReadable(Task.PRIORITY_NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-output-dir.pom.xml").toURI(), Task.PRIORITY_NORMAL, pomLoader, false);
			MavenPOM pom = load.blockResult(30000);
			
			File classes = pom.getClasses().blockResult(30000);
			Assert.assertNotNull(classes);
			Assert.assertEquals(new File("./target/test-out").getAbsolutePath(), classes.getAbsolutePath());
		} finally {
			outFile.delete();
		}
	}
	
	@Test(timeout=120000)
	public void testLoadFile() throws Exception {
		Assert.assertNotNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, null));
		Assert.assertNotNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), null, null, Task.PRIORITY_NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junitXX", "junit", junit.runner.Version.id(), null, null));
		Assert.assertNull(repo.loadFile("junitXX", "junit", junit.runner.Version.id(), null, null, Task.PRIORITY_NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junitXX", junit.runner.Version.id(), null, null));
		Assert.assertNull(repo.loadFile("junit", "junitXX", junit.runner.Version.id(), null, null, Task.PRIORITY_NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", "XX", null, null));
		Assert.assertNull(repo.loadFile("junit", "junit", "XX", null, null, Task.PRIORITY_NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "XX", null));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "XX", null, Task.PRIORITY_NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, "test-jar"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), null, "test-jar", Task.PRIORITY_NORMAL).blockResult(30000));
	}
	
}
