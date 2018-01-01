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
		PropertiesUtil.resolve(properties);
		Assert.assertEquals("Hello", properties.get("toto"));
		Assert.assertEquals("World", properties.get("titi"));
		Assert.assertEquals("Hello World", properties.get("sample"));
		Assert.assertEquals("Sample is composed of Hello and World to make an Hello World Message",
			PropertiesUtil.resolve("Sample is composed of ${toto} and ${titi} to make an ${sample} Message", properties));
	}
	
}
