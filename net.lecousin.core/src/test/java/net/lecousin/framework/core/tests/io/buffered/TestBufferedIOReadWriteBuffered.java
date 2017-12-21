package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWriteBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.BufferedIO;

@RunWith(Parameterized.class)
public class TestBufferedIOReadWriteBuffered extends TestReadWriteBuffered {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestBufferedIOReadWriteBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	protected BufferedIO.ReadWrite openReadWriteBuffered() throws Exception {
		File tmpFile = File.createTempFile("test", "bufferedio.rw");
		tmpFile.deleteOnExit();
		return new BufferedIO.ReadWrite(new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL), 4096, 0);
	}
	
}
