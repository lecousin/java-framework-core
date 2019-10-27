package net.lecousin.framework.core.tests.application;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionRange;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestVersion extends LCCoreAbstractTest {

	@Test
	public void testRanges() {
		Assert.assertTrue(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.3")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3-alpha")).includes(new Version("1.2.3-beta")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.4")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.3.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("2.2.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("0.2.3")));
		Assert.assertEquals("1.2.3", new VersionRange(new Version("1.2.3")).toString());
		Assert.assertEquals("[1.2-2.4]", new VersionRange(new Version("1.2"), new Version("2.4"), true).toString());
		Assert.assertEquals("[2.1-2.3[", new VersionRange(new Version("2.1"), new Version("2.3"), false).toString());
		Assert.assertEquals("[2.1", new VersionRange(new Version("2.1"), null, false).toString());
		
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.3")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.4")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.7")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.0")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.1")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.5")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.6")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.7")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.1.5")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.4.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("2.0")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("2.3.2")));

		Assert.assertTrue(new VersionRange(null, new Version("1.3.6"), true).includes(new Version("1.3")));
		Assert.assertTrue(new VersionRange(null, new Version("1.3.6"), true).includes(new Version("0.2")));
		Assert.assertFalse(new VersionRange(null, new Version("1.3.6"), true).includes(new Version("1.3.7")));
		Assert.assertFalse(new VersionRange(null, new Version("1.3.6"), true).includes(new Version("1.4")));
		Assert.assertFalse(new VersionRange(null, new Version("1.3.6"), true).includes(new Version("2.1")));

		Assert.assertTrue(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("1.4")));
		Assert.assertTrue(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("2.1")));
		Assert.assertTrue(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("1.3.6")));
		Assert.assertFalse(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("1.3.5")));
		Assert.assertFalse(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("1.2.3")));
		Assert.assertFalse(new VersionRange(new Version("1.3.6"), null, true).includes(new Version("0.4")));
	}
	
	@Test
	public void testVersion() {
		Assert.assertEquals(0, new Version("1.2.3.4.5-alpha").compareTo(new Version("1.2.3.4.5-beta")));
		Assert.assertEquals(new Version("1.2.3.4.5-alpha").hashCode(), new Version("1.2.3.4.5-beta").hashCode());
		Assert.assertTrue(new Version("1.2.3.4.5-alpha").equals(new Version("1.2.3.4.5-beta")));
		Assert.assertFalse(new Version("1.2.3.4.5-alpha").equals(new Version("1.2.3.4.6-beta")));
		Assert.assertFalse(new Version("1.2.3.4.5-alpha").equals(new Object()));
		Assert.assertEquals("1.2.3.4.5-alpha", new Version("1.2.3.4.5-alpha").toString());
		Assert.assertEquals("1|beta", new Version("1|beta").toString());
		Assert.assertEquals("pi", new Version("pi").toString());
		Assert.assertEquals(1, Version.compare(new int[] { 1,  2, 3 }, new int[] { 1, 2 }));
	}
	
	@Test
	public void testSpec() {
		Version v = new Version("3.7.12.56");
		VersionSpecification.SingleVersion s = new VersionSpecification.SingleVersion(v);
		Assert.assertEquals(v, s.getVersion());
		Assert.assertTrue(s.isMatching(v));
		Assert.assertFalse(s.isMatching(new Version("3.2")));
		Assert.assertEquals(0, s.compare(v, v));
		Assert.assertEquals("3.7.12.56", s.toString());
		
		VersionRange range = new VersionRange(new Version("3.6"), new Version("3.8"), true);
		VersionSpecification.Range r = new VersionSpecification.Range(range);
		Assert.assertEquals(range, r.getRange());
		Assert.assertTrue(r.isMatching(v));
		Assert.assertFalse(r.isMatching(new Version("3.70")));
		Assert.assertEquals(0, r.compare(v, v));
		Assert.assertTrue(r.compare(v, new Version("3.6")) > 0);
		Assert.assertTrue(r.compare(v, new Version("3.8")) < 0);
		Assert.assertEquals("[3.6-3.8]", r.toString());
		
		VersionSpecification.RangeWithRecommended rr = new VersionSpecification.RangeWithRecommended(range, v);
		Assert.assertEquals(range, rr.getRange());
		Assert.assertEquals(v, rr.getRecommended());
		Assert.assertTrue(rr.isMatching(new Version("3.6")));
		Assert.assertTrue(rr.isMatching(new Version("3.7")));
		Assert.assertTrue(rr.isMatching(new Version("3.8")));
		Assert.assertTrue(rr.isMatching(new Version("3.7.12.56")));
		Assert.assertTrue(rr.compare(new Version("3.6"), new Version("3.7")) < 0);
		Assert.assertTrue(rr.compare(new Version("3.8"), new Version("3.7")) > 0);
		Assert.assertTrue(rr.compare(new Version("3.7.12.56"), new Version("3.7")) > 0);
		Assert.assertTrue(rr.compare(new Version("3.7"), new Version("3.7.12.56")) < 0);
		Assert.assertTrue(rr.compare(new Version("3.7.12.56"), new Version("3.6")) > 0);
		Assert.assertTrue(rr.compare(new Version("3.7.12.56"), new Version("3.8")) > 0);
		rr.toString();
	}
	
}
