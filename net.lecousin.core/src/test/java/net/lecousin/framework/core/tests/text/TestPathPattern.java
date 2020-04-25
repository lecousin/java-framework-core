package net.lecousin.framework.core.tests.text;

import java.util.Arrays;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.text.pattern.path.PathPattern;

import org.junit.Assert;
import org.junit.Test;

public class TestPathPattern extends LCCoreAbstractTest {

	@Test
	public void testPatterns() {
		test("", "path", false);
		test("path", "path", true);
		test("toto/t?t?", "path", false);
		test("toto/t?t?", "toto", false);
		test("toto/t?t?", "toto/hello", false);
		test("toto/t?t?", "toto/titi", true);
		test("toto/t?t?", "toto/tttt", true);
		test("toto/t?t?", "toto/tttt2", false);
		test("toto/*/t?t?", "toto/tttt/tttt", true);
		test("toto/*/t?t?", "toto/hello/tttt", true);
		test("toto/*/t?t?", "toto/hello/hello", false);
		test("toto/*/t?t?", "toto/tttt/hello", false);
		test("toto/*/t?t?", "toto/hello/hello/tata", false);
		test("toto/**/t?t?", "toto/hello/hello/tata", true);
		test("toto/**/t?t?", "toto/tata", true);
		test("toto/**/t?t?", "toto/tata/tutu", true);
		test("toto/**/t?t?", "toto/tata/hello", false);
	}
	
	@SuppressWarnings("boxing")
	public void test(String pattern, String path, boolean expectedMatch) {
		PathPattern p = new PathPattern(pattern);
		Assert.assertEquals(expectedMatch, p.matches(path));
		Assert.assertEquals(expectedMatch, p.matches(path.split("/")));
		Assert.assertEquals(expectedMatch, p.matches(Arrays.asList(path.split("/"))));
	}
	
}
