package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.MemoryIO;

@RunWith(Parameterized.class)
public class TestMemoryIOReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestMemoryIOReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected MemoryIO createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		MemoryIO io = new MemoryIO(4096, "test");
		IOUtil.copy(file, io, fileSize, false, null, 0).blockThrow(0);
		file.close();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
}
