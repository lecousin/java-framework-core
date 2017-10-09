package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteBuffersIO;

@RunWith(Parameterized.class)
public class TestByteBuffersIOReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestByteBuffersIOReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected ByteBuffersIO createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		ByteBuffersIO io = new ByteBuffersIO(true, "test", Task.PRIORITY_NORMAL);
		IOUtil.copy(file, io, fileSize, false, null, 0).block(0);
		file.close();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
}
