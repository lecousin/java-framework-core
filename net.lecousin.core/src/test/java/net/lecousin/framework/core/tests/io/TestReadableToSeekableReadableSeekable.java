package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.buffering.ReadableToSeekable;

@RunWith(Parameterized.class)
public class TestReadableToSeekableReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestReadableToSeekableReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(ReadOnly file, long fileSize) throws Exception {
		PreBufferedReadable ioBuf = new PreBufferedReadable(file, 256, Task.PRIORITY_IMPORTANT, 512, Task.PRIORITY_IMPORTANT, 10);
		return new ReadableToSeekable(ioBuf, 512);
	}
	
}
