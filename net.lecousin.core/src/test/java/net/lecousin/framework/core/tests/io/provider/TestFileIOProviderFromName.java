package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.provider.FileIOProviderFromName;
import net.lecousin.framework.io.provider.IOProviderFromName;
import net.lecousin.framework.io.provider.IOProviderFromName.Readable.SubPath;

import org.junit.Assert;
import org.junit.Test;

public class TestFileIOProviderFromName extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		File f = TemporaryFiles.get().createFileSync("test", "fileioproviderfromname");
		FileIOProviderFromName provider = new FileIOProviderFromName(f.getParentFile());
		provider.provideReadableIO(f.getName(), Task.PRIORITY_NORMAL).close();
		
		IOProviderFromName.Readable.SubPath subProvider = new SubPath(provider, "toto");
		Assert.assertNull(subProvider.provideReadableIO("titi", Task.PRIORITY_NORMAL));
	}
	
}
