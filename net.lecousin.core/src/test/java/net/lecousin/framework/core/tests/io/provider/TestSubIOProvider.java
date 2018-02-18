package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.provider.SubIOProvider;

import org.junit.Assert;
import org.junit.Test;

public class TestSubIOProvider extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		ByteArrayIO io = new ByteArrayIO(new byte[200], "test");
		SubIOProvider.Readable provider = new SubIOProvider.Readable(io, 10, 20, "test");
		provider.provideIOReadable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekable(Task.PRIORITY_NORMAL).close();
		Assert.assertEquals("test", provider.getDescription());
		io.close();
	}
	
}
