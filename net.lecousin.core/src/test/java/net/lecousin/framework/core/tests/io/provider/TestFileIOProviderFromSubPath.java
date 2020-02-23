package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.provider.FileIOProviderFromSubPath;

import org.junit.Test;

public class TestFileIOProviderFromSubPath extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		File f = TemporaryFiles.get().createFileSync("test", "fileioproviderfromname");
		FileIOProviderFromSubPath provider = new FileIOProviderFromSubPath(f.getParentFile());
		provider.get(f.getName()).provideIOReadable(Task.Priority.NORMAL).close();
	}
	
}
