package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.io.FileIO;

@RunWith(Parameterized.class)
public class TestFileIOReadWrite extends TestReadWrite {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestFileIOReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected FileIO.ReadWrite openReadWrite() throws Exception {
		File tmpFile = File.createTempFile("test", "fileio.rw");
		tmpFile.deleteOnExit();
		return new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL);
	}
	
}
