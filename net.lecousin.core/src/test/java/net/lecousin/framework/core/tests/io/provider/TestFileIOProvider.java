package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.provider.FileIOProvider;

import org.junit.Test;

public class TestFileIOProvider extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		File f = TemporaryFiles.get().createFileSync("test", "fileioprovider");
		FileIOProvider provider = new FileIOProvider(f);
		provider.provideIOReadable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadableSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWrite(Task.PRIORITY_NORMAL).close();
		provider.provideIOReadWriteSeekable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritable(Task.PRIORITY_NORMAL).close();
		provider.provideIOWritableSeekable(Task.PRIORITY_NORMAL).close();
	}
	
}
