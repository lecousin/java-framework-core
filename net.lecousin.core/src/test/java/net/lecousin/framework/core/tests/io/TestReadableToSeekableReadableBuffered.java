package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.buffering.ReadableToSeekable;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestReadableToSeekableReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestReadableToSeekableReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf, 4096);
	}
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		PreBufferedReadable ioBuf = new PreBufferedReadable(file, bufferingSize / 2, Task.PRIORITY_IMPORTANT, bufferingSize, Task.PRIORITY_IMPORTANT, 10);
		return new ReadableToSeekable(ioBuf, bufferingSize * 2 / 3);
	}
	
}
