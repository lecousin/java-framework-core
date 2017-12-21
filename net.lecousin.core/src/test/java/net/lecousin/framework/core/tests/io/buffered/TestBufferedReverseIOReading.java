package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableByteStream;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.ReadableByteStream;
import net.lecousin.framework.io.buffering.BufferedReverseIOReading;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestBufferedReverseIOReading extends TestReadableByteStream {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestBufferedReverseIOReading(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected ReadableByteStream createReadableByteStreamFromFile(ReadOnly file, long fileSize) throws Exception {
		BufferedReverseIOReading io = new BufferedReverseIOReading(openFile(), 512);
		goBackward(io);
		return io;
	}
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createReadableByteStreamFromFile(openFile(), getFileSize());
	}
	
	private void goBackward(BufferedReverseIOReading rio) {
		for (int i = nbBuf - 1; i >= 0; --i) {
			for (int j = testBuf.length - 1; j >= 0; --j) {
				int c;
				try { c = rio.readReverse(); }
				catch (Throwable t) {
					throw new AssertionError("Error at " + (i * testBuf.length + j), t);
				}
				if (c != (testBuf[j] & 0xFF))
					throw new AssertionError("Invalid character " + c + " (" + (char)c + ") at " + (i * testBuf.length + j));
			}
		}
	}
	
}
