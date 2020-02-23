package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.ByteArrayIOProvider;

import org.junit.Assert;
import org.junit.Test;

public class TestByteArrayIOProvider extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		ByteArrayIOProvider provider = new ByteArrayIOProvider(new byte[100], "test");
		provider.provideIOReadable(Task.Priority.NORMAL).close();
		provider.provideIOReadableSeekable(Task.Priority.NORMAL).close();
		provider.provideIOReadableSeekableKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOReadWrite(Task.Priority.NORMAL).close();
		provider.provideIOReadWriteSeekable(Task.Priority.NORMAL).close();
		provider.provideIOWritable(Task.Priority.NORMAL).close();
		provider.provideIOWritableSeekable(Task.Priority.NORMAL).close();
		Assert.assertEquals("test", provider.getDescription());
	}
	
}
