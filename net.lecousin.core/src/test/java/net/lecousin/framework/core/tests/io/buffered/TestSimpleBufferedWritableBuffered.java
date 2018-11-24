package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;

@RunWith(Parameterized.class)
public class TestSimpleBufferedWritableBuffered extends TestWritableBuffered {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestSimpleBufferedWritableBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@Override
	protected IO.Writable.Buffered createWritableBuffered() throws IOException {
		file = createFile();
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		return new SimpleBufferedWritable(fileIO, 4096);
	}

	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, 0);
	}
}
