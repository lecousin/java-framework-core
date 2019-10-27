package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.ByteArrayIOProvider;

import org.junit.Assert;
import org.junit.Test;

public class TestByteArrayIOProvider extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		ByteArrayIOProvider provider = new ByteArrayIOProvider(new byte[100], "test");
		provider.provideIOReadable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekableKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWrite(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritableSeekable(Task.PRIORITY_NORMAL).close();
		Assert.assertEquals("test", provider.getDescription());
	}
	
}
