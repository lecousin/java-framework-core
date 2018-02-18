package net.lecousin.framework.core.tests.concurrent;

import java.io.File;

import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestDrivesTaskManager extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void simpleTests() {
		Object res1 = Threading.getDrivesTaskManager().getResource(new File("."));
		Object res2 = Threading.getDrivesTaskManager().getResource(new File(".").getAbsolutePath());
		Assert.assertEquals(res1, res2);
		
		Threading.getDrivesTaskManager().getResources();
	}
	
}
