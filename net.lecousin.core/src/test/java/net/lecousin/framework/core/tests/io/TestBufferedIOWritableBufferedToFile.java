package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBufferedToFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;

@RunWith(Parameterized.class)
public class TestBufferedIOWritableBufferedToFile extends TestWritableBufferedToFile {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestBufferedIOWritableBufferedToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Writable.Buffered createWritableBufferedFromFile(File file) throws IOException {
		return new BufferedIO.ReadWrite(new FileIO.ReadWrite(file, Task.PRIORITY_NORMAL), 4096, 0);
	}
	
}
