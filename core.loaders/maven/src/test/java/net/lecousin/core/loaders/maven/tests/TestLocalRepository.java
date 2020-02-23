package net.lecousin.core.loaders.maven.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import net.lecousin.core.loaders.maven.MavenLocalRepository;
import net.lecousin.core.loaders.maven.MavenPOM;
import net.lecousin.core.loaders.maven.MavenPOMLoader;
import net.lecousin.core.loaders.maven.MavenSettings;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification.SingleVersion;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.application.libraries.artifacts.LibrariesRepository;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptor;
import net.lecousin.framework.application.libraries.artifacts.LibraryDescriptorLoader.DependencyNode;
import net.lecousin.framework.collections.Tree;
import net.lecousin.framework.collections.Tree.Node;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.io.provider.TestURIProvider;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteArrayIO;
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
	
	@Test
	public void testPOMLoader() throws Exception {
		Assert.assertTrue(pomLoader.detect(new File(".")));
		
		try {
			pomLoader.loadLibrary("does", "not", new SingleVersion(new Version("1")), Task.Priority.NORMAL, new ArrayList<>(0)).blockResult(15000);
			throw new AssertionError("Error expected");
		} catch (LibraryManagementException e) {
			// ok
		}
		
		Map<Version, List<Node<DependencyNode>>> artifactVersions = new HashMap<>();
		Tree.WithParent<DependencyNode> tree = new Tree.WithParent<>(null);
		List<Node<DependencyNode>> list = new LinkedList<>();
		Node<DependencyNode> node = tree.add(new DependencyNode(new TestDependency("g1", "a1", new SingleVersion(new Version("1")), null, false, null, null)));
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g2", "a2", new SingleVersion(new Version("1")), null, false, null, null)));
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g3", "a3", new SingleVersion(new Version("1")), null, false, null, null)));
		list.add(node);
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g4", "a4", new SingleVersion(new Version("1")), null, false, null, null)));
		
		node = tree.add(new DependencyNode(new TestDependency("g5", "a5", new SingleVersion(new Version("1")), null, false, null, null)));
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g6", "a6", new SingleVersion(new Version("1")), null, false, null, null)));
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g3", "a3", new SingleVersion(new Version("1")), null, false, null, null)));
		list.add(node);
		
		artifactVersions.put(new Version("1"), list);
		
		list = new LinkedList<>();
		node = tree.add(new DependencyNode(new TestDependency("g7", "a7", new SingleVersion(new Version("1")), null, false, null, null)));
		node = node.getSubNodes().add(new DependencyNode(new TestDependency("g3", "a3", new SingleVersion(new Version("1.1")), null, false, null, null)));
		list.add(node);
		artifactVersions.put(new Version("1.1"), list);
		
		Assert.assertEquals("1.1", pomLoader.resolveVersionConflict("g3", "a3", artifactVersions).toString());
	}
	
	@Test
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
	
	@Test
	public void testJUnitVersion() throws Exception {
		List<String> versions = repo.getAvailableVersions("junit", "junit", Task.Priority.NORMAL).blockResult(0);
		Assert.assertNotNull(versions);
		if (!versions.contains(junit.runner.Version.id()))
			throw new AssertionError("Available versions of junit does not contain " + junit.runner.Version.id() + ": " + versions.toString());
	}
	
	@Test
	public void testUnknownArtifactVersion() throws Exception {
		List<String> versions = repo.getAvailableVersions("junit", "doesnotexist", Task.Priority.NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
		versions = repo.getAvailableVersions("doesnotexist", "doesnotexist", Task.Priority.NORMAL).blockResult(0);
		Assert.assertTrue(versions == null || versions.isEmpty());
	}
	
	@Test
	public void testLoadJUnit() throws Exception {
		MavenPOM pom = repo.load("junit", "junit", junit.runner.Version.id(), pomLoader, Task.Priority.NORMAL).blockResult(0);
		Assert.assertEquals("junit", pom.getGroupId());
		Assert.assertEquals("junit", pom.getArtifactId());
		Assert.assertEquals(junit.runner.Version.id(), pom.getVersionString());
		
		File file = pom.getClasses().blockResult(0);
		Assert.assertNotNull(file);
		ZipFile zip = new ZipFile(file);
		Assert.assertNotNull(zip.getEntry("junit/runner/Version.class"));
		zip.close();
	}

	@Test
	public void testLoadUnknownArtifact() throws Exception {
		try {
			MavenPOM pom = repo.load("junit", "doesnotexist", junit.runner.Version.id(), pomLoader, Task.Priority.NORMAL).blockResult(0);
			if (pom != null)
				throw new AssertionError("should fail");
		} catch (Exception e) {
			// ok
		}
		
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("/doesnotexist.pom").toURI(), Task.Priority.NORMAL, pomLoader, false);
		Assert.assertTrue(load.hasError() && load.getError().getCause() instanceof FileNotFoundException);
	}
	
	@Test
	public void testDependencyWithSystemPath() throws Exception {
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new URI("classpath:test-maven/test-system-path.pom.xml"), Task.Priority.NORMAL, pomLoader, false);
		MavenPOM pom = load.blockResult(30000);
		pom.getLoader();
		pom.getDirectory();
		List<LibraryDescriptor.Dependency> deps = pom.getAllDependenciesAnyScope();
		Assert.assertFalse(pom.hasClasses());
		Assert.assertNull(pom.getClasses().blockResult(15000));
		Assert.assertEquals(3, deps.size());
		for (LibraryDescriptor.Dependency dep : deps) {
			if (dep.getArtifactId().equals("depsys")) {
				Assert.assertEquals("/test", dep.getKnownLocation().toString());
				Assert.assertEquals("test", dep.getGroupId());
				Assert.assertTrue(dep.getVersionSpecification().isMatching(new Version("1")));
				Assert.assertNull(dep.getClassifier());
				Assert.assertFalse(dep.isOptional());
				Assert.assertEquals(0, dep.getExcludedDependencies().size());
			} else if (dep.getArtifactId().equals("depsys2")) {
				Assert.assertEquals("file://test", dep.getKnownLocation().toString());
				Assert.assertEquals("test", dep.getGroupId());
				Assert.assertTrue(dep.getVersionSpecification().isMatching(new Version("1")));
				Assert.assertEquals("classified", dep.getClassifier());
				Assert.assertTrue(dep.isOptional());
				Assert.assertEquals(0, dep.getExcludedDependencies().size());
			} else if (dep.getArtifactId().equals("depsys3")) {
				Assert.assertNull("/test", dep.getKnownLocation());
				Assert.assertEquals("test", dep.getGroupId());
				Assert.assertTrue(dep.getVersionSpecification().isMatching(new Version("1")));
				Assert.assertNull(dep.getClassifier());
				Assert.assertFalse(dep.isOptional());
				Assert.assertEquals(1, dep.getExcludedDependencies().size());
				Assert.assertEquals("groupExcluded", dep.getExcludedDependencies().get(0).getValue1());
				Assert.assertEquals("artifactExcluded", dep.getExcludedDependencies().get(0).getValue2());
			} else {
				throw new Exception("Unexpected dependency: " + dep.getArtifactId());
			}
		}
	}
	
	@Test
	public void testMyPOM() throws Exception {
		AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
		MavenPOM pom = load.blockResult(30000);
		Assert.assertEquals(pom.getArtifactId(), "maven");
		
		File classes = pom.getClasses().blockResult(30000);
		Assert.assertNotNull(classes);
		Assert.assertEquals(new File("./target/classes").getAbsolutePath(), classes.getAbsolutePath());
		
		AsyncSupplier<? extends LibraryDescriptor, LibraryManagementException> load2 = pomLoader.loadProject(new File("."), Task.Priority.NORMAL);
		LibraryDescriptor lib = load2.blockResult(30000);
		Assert.assertEquals(lib.getArtifactId(), "maven");
		lib.getDependencies();
	}
	
	@Test
	public void testPOMWithOutputDirectory() throws Exception {
		File outFile = new File("./test-output-dir.pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.Priority.NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/test-output-dir.pom.xml"))).provideIOReadable(Task.Priority.NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-output-dir.pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
			MavenPOM pom = load.blockResult(30000);
			
			File classes = pom.getClasses().blockResult(30000);
			Assert.assertNotNull(classes);
			Assert.assertEquals(new File("./target/test-out").getAbsolutePath(), classes.getAbsolutePath());
			pom.getDependencies();
		} finally {
			outFile.delete();
		}
	}
	
	@Test
	public void testPOMWithRepositories() throws Exception {
		File outFile = new File("./test-repositories.pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.Priority.NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/test-repositories.pom.xml"))).provideIOReadable(Task.Priority.NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-repositories.pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
			MavenPOM pom = load.blockResult(30000);
			
			List<LibrariesRepository> repos = pom.getDependenciesAdditionalRepositories();
			Assert.assertEquals(2, repos.size());
			pom.toString();
		} finally {
			outFile.delete();
		}
	}
	
	@Test
	public void testPOMWithAdditionalFields() throws Exception {
		File outFile = new File("./test-additional-fields.pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.Priority.NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/test-additional-fields.pom.xml"))).provideIOReadable(Task.Priority.NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-additional-fields.pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
			load.blockResult(30000);
		} finally {
			outFile.delete();
		}
	}

	@Test
	public void testPOMWithProperties() throws Exception {
		File outFile = new File("./test-properties.pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.Priority.NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/test-properties.pom.xml"))).provideIOReadable(Task.Priority.NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-properties.pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
			load.blockResult(30000);
		} finally {
			outFile.delete();
		}
	}
	
	private void testError(String errorName) throws IOException, CancelException, URISyntaxException {
		File outFile = new File("./test-error-" + errorName + ".pom.xml");
		outFile.createNewFile();
		outFile.deleteOnExit();
		try {
			new File("./target/test-out").mkdir();
			FileIO.WriteOnly out = new FileIO.WriteOnly(outFile, Task.Priority.NORMAL);
			IOUtil.copy(
				((IOProvider.Readable)IOProviderFromURI.getInstance().get(new URI("classpath:test-maven/error/test-" + errorName + ".pom.xml"))).provideIOReadable(Task.Priority.NORMAL),
				out,
				-1, true, null, 0).blockThrow(15000);
			
			AsyncSupplier<MavenPOM, LibraryManagementException> load = MavenPOM.load(new File("./test-error-" + errorName + ".pom.xml").toURI(), Task.Priority.NORMAL, pomLoader, false);
			try {
				load.blockResult(30000);
				throw new AssertionError("Error expected for pom " + errorName);
			} catch (LibraryManagementException e) {
				// ok
			}
		} finally {
			outFile.delete();
		}
	}
	
	@Test
	public void testErrors() throws Exception {
		testError("invalid-root");
		testError("invalid-xml");
		testError("invalid-xml2");
		testError("invalid-xml3");
		testError("invalid-xml4");
		testError("invalid-xml5");
		testError("empty");
	}
	
	private void testIOError(String path) throws Exception {
		AsyncSupplier<MavenPOM, LibraryManagementException> load =
			MavenPOM.load(new URI("test://" + path), Task.Priority.NORMAL, pomLoader, false);
		try {
			load.blockResult(30000);
			throw new AssertionError("Error expected for pom " + path);
		} catch (LibraryManagementException e) {
			// ok
		}
	}
	
	@Test
	public void testIOErrors() throws Exception {
		testIOError("/errors/provider/readable");
		testIOError("/errors/provider/writable");
		testIOError("/errors/always/readable");
		testIOError("/errors/always/readable/knownsize");
		testIOError("/readable/empty");
	}
	
	@Test
	public void testFromTest() throws Exception {
		TestURIProvider.getInstance().register("/mypom", new IOProvider.Readable() {
			@Override
			public String getDescription() {
				return "mypom";
			}
			@Override
			public IO.Readable provideIOReadable(Priority priority) throws IOException {
				return new ByteArrayIO("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><parent><relativePath>..</relativePath></parent></project>".getBytes(), "mypom");
			}
		});
		AsyncSupplier<MavenPOM, LibraryManagementException> load =
			MavenPOM.load(new URI("test:///mypom"), Task.Priority.NORMAL, pomLoader, false);
		MavenPOM pom = load.blockResult(5000);
		Assert.assertNull(pom.getClasses().blockResult(0));
	}
	
	@Test
	public void testLoadFile() throws Exception {
		Assert.assertNotNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, null));
		Assert.assertNotNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), null, null, Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junitXX", "junit", junit.runner.Version.id(), null, null));
		Assert.assertNull(repo.loadFile("junitXX", "junit", junit.runner.Version.id(), null, null, Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junitXX", junit.runner.Version.id(), null, null));
		Assert.assertNull(repo.loadFile("junit", "junitXX", junit.runner.Version.id(), null, null, Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", "XX", null, null));
		Assert.assertNull(repo.loadFile("junit", "junit", "XX", null, null, Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "XX", null));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "XX", null, Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, "test-jar"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), null, "test-jar", Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "t", "test-jar"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "t", "test-jar", Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, "ejb-client"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), null, "ejb-client", Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "t", "ejb-client"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "t", "ejb-client", Task.Priority.NORMAL).blockResult(30000));

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "t", "ejb"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "t", "ejb", Task.Priority.NORMAL).blockResult(30000));

		repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, "java-source");
		repo.loadFile("junit", "junit", junit.runner.Version.id(), null, "java-source", Task.Priority.NORMAL).blockResult(30000);

		repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "t", "java-source");
		repo.loadFile("junit", "junit", junit.runner.Version.id(), "t", "java-source", Task.Priority.NORMAL).blockResult(30000);

		repo.loadFileSync("junit", "junit", junit.runner.Version.id(), null, "javadoc");
		repo.loadFile("junit", "junit", junit.runner.Version.id(), null, "javadoc", Task.Priority.NORMAL).blockResult(30000);

		repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "t", "javadoc");
		repo.loadFile("junit", "junit", junit.runner.Version.id(), "t", "javadoc", Task.Priority.NORMAL).blockResult(30000);

		Assert.assertNull(repo.loadFileSync("junit", "junit", junit.runner.Version.id(), "", "unknown"));
		Assert.assertNull(repo.loadFile("junit", "junit", junit.runner.Version.id(), "", "unknown", Task.Priority.NORMAL).blockResult(30000));
	}
	
}
