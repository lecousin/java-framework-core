package net.lecousin.framework.core.tests.application;

import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.ArtifactReference;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestArtifact extends LCCoreAbstractTest {

	@Test
	public void test() {
		Artifact a1 = new Artifact("mygroup", "myartifact", new Version("0.0.0"));
		Artifact a2 = new Artifact(a1);
		Assert.assertTrue(a1.equals(a2));
		Assert.assertEquals(a1.hashCode(), a2.hashCode());
		Assert.assertFalse(a1.equals(new Object()));
		Assert.assertFalse(a1.equals(new Artifact("mygroup", "myartifact", new Version("0.1.0"))));
		Assert.assertFalse(a1.equals(new Artifact("mygroup", "myartifact2", new Version("0.0.0"))));
		Assert.assertFalse(a1.equals(new Artifact("mygroup2", "myartifact", new Version("0.0.0"))));
		
		ArtifactReference ref = new ArtifactReference("mygroup", "myartifact");
		Assert.assertTrue(ref.matches("mygroup", "myartifact"));
		Assert.assertFalse(ref.matches("mygroup", "myartifact2"));
		Assert.assertFalse(ref.matches("mygroup2", "myartifact"));
		ref = new ArtifactReference("mygroup", "*");
		Assert.assertTrue(ref.matches("mygroup", "myartifact"));
		Assert.assertTrue(ref.matches("mygroup", "myartifact2"));
		Assert.assertFalse(ref.matches("mygroup2", "myartifact"));
		ref = new ArtifactReference("*", "myartifact");
		Assert.assertTrue(ref.matches("mygroup", "myartifact"));
		Assert.assertFalse(ref.matches("mygroup", "myartifact2"));
		Assert.assertTrue(ref.matches("mygroup2", "myartifact"));
		ref = new ArtifactReference("*", "*");
		Assert.assertTrue(ref.matches("mygroup", "myartifact"));
		Assert.assertTrue(ref.matches("mygroup", "myartifact2"));
		Assert.assertTrue(ref.matches("mygroup2", "myartifact"));
	}
	
}
