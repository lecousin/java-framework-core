package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

@RunWith(Parameterized.class)
public class TestByteArrayIOReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestByteArrayIOReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected ByteArrayIO createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		byte[] b = new byte[(int)fileSize];
		if (file.readFullySync(ByteBuffer.wrap(b)) != fileSize)
			throw new Exception("Error loading file into memory");
		file.close();
		ByteArrayIO io = new ByteArrayIO(b, b.length, "Test");
		return io;
	}
	
}