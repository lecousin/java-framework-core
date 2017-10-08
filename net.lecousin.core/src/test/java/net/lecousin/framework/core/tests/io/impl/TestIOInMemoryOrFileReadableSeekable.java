package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;

@RunWith(Parameterized.class)
public class TestIOInMemoryOrFileReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestIOInMemoryOrFileReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected IOInMemoryOrFile createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		IOInMemoryOrFile io = new IOInMemoryOrFile(4*1024*1024, Task.PRIORITY_NORMAL, "test");
		IOUtil.copy(file, io, fileSize, false, null, 0).block(0);
		file.close();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
}
