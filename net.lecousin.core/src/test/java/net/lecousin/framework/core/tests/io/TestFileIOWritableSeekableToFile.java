package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableSeekableToFile;
import net.lecousin.framework.io.FileIO;

@RunWith(Parameterized.class)
public class TestFileIOWritableSeekableToFile extends TestWritableSeekableToFile {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestFileIOWritableSeekableToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected FileIO.WriteOnly createWritableSeekableFromFile(File file) throws IOException {
		return new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
	}
	
}