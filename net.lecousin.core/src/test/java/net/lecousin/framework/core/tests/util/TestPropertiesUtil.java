package net.lecousin.framework.core.tests.util;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.PropertiesUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestPropertiesUtil extends LCCoreAbstractTest {

	@Test
	public void testProperties() {
		Map<String, String> properties = new HashMap<>();
		properties.put("toto", "Hello");
		properties.put("titi", "World");
		properties.put("sample", "${toto} ${titi}");
		properties.put("aa", "${toto} ${titi");
		properties.put("n", null);
		properties.put("nn", "test ${n} 2");
		PropertiesUtil.resolve(properties);
		Assert.assertEquals("Hello", properties.get("toto"));
		Assert.assertEquals("World", properties.get("titi"));
		Assert.assertEquals("Hello World", properties.get("sample"));
		Assert.assertEquals("Hello ${titi", properties.get("aa"));
		Assert.assertEquals("test  2", properties.get("nn"));
		Assert.assertEquals("Sample is composed of Hello and World to make an Hello World Message",
			PropertiesUtil.resolve("Sample is composed of ${toto} and ${titi} to make an ${sample} Message", properties));
		Assert.assertEquals("Test Hello ${titi .", PropertiesUtil.resolve("Test ${n}${toto} ${titi .", properties));
	}
	
}
