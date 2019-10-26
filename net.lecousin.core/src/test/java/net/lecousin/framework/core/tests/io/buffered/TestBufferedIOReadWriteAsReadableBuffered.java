package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestBufferedIOReadWriteAsReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, bufferingSize = {3}")
	public static Collection<Object[]> parameters() {
		return TestReadableBuffered.generateTestCases(false);
	}
	
	public TestBufferedIOReadWriteAsReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferingSize) {
		super(testFile, testBuf, nbBuf, bufferingSize);
	}
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		File f = file.getFile();
		file.close();
		FileIO.ReadWrite frw = new FileIO.ReadWrite(f, Task.PRIORITY_NORMAL);
		return new BufferedIO.ReadWrite(frw, fileSize, bufferingSize / 2, bufferingSize, true);
	}
	
}
