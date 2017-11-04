package net.lecousin.framework.core.tests.io;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;

@RunWith(Parameterized.class)
public class TestIOInMemoryOrFileReadWrite extends TestReadWrite {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestIOInMemoryOrFileReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected IOInMemoryOrFile openReadWrite() {
		return new IOInMemoryOrFile(testBuf.length*(nbBuf/2)-testBuf.length/2, Task.PRIORITY_NORMAL, "test file");
	}
	
}
