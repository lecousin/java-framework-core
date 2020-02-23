package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.concurrent.threads.Task;
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
		provider.provideIOReadable(Task.Priority.NORMAL).close();
		provider.provideIOReadableKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOReadableSeekable(Task.Priority.NORMAL).close();
		provider.provideIOReadableSeekableKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOReadWrite(Task.Priority.NORMAL).close();
		provider.provideIOReadWriteKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOReadWriteSeekable(Task.Priority.NORMAL).close();
		provider.provideIOReadWriteSeekableKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOReadWriteSeekableResizable(Task.Priority.NORMAL).close();
		provider.provideIOWritable(Task.Priority.NORMAL).close();
		provider.provideIOWritableSeekable(Task.Priority.NORMAL).close();
		provider.provideIOWritableSeekableKnownSize(Task.Priority.NORMAL).close();
		provider.provideIOWritableSeekableResizable(Task.Priority.NORMAL).close();
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
