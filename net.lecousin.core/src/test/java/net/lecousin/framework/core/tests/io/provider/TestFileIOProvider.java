package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.provider.FileIOProvider;

import org.junit.Assert;
import org.junit.Test;

public class TestFileIOProvider extends LCCoreAbstractTest {

	@SuppressWarnings("unused")
	@Test
	public void test() throws Exception {
		File f = TemporaryFiles.get().createFileSync("test", "fileioprovider");
		FileIOProvider provider = new FileIOProvider(f);
		provider.provideIOReadable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekableKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWrite(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteSeekableKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteSeekableResizable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritableSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritableSeekableKnownSize(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritableSeekableResizable(Task.PRIORITY_NORMAL).close();
		Assert.assertEquals(f.getAbsolutePath(), provider.getDescription());
		Assert.assertEquals(f, provider.getFile());
		try {
			new FileIOProvider(new File("."));
			throw new AssertionError("Must not accept a directory");
		} catch (IllegalArgumentException e) {
			// ok
		}
	}
	
}
