package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.MemoryIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestMemoryIOReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, bufferingSize = {3}")
	public static Collection<Object[]> parameters() {
		return TestReadableBuffered.generateTestCases(true);
	}
	
	public TestMemoryIOReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferingSize) {
		super(testFile, testBuf, nbBuf, bufferingSize);
	}
	
	@Override
	protected MemoryIO createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		MemoryIO io = new MemoryIO(bufferingSize, "test");
		IOUtil.copy(file, io, fileSize, false, null, 0).blockThrow(0);
		file.close();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
}
