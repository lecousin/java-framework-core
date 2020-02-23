package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.provider.SubIOProvider;

import org.junit.Assert;
import org.junit.Test;

public class TestSubIOProvider extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		ByteArrayIO io = new ByteArrayIO(new byte[200], "test");
		SubIOProvider.Readable provider = new SubIOProvider.Readable(io, 10, 20, "test");
		provider.provideIOReadable(Task.Priority.NORMAL).close();
		provider.provideIOReadableSeekable(Task.Priority.NORMAL).close();
		Assert.assertEquals("test", provider.getDescription());
		io.close();
	}
	
}
