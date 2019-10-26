package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.TwoBuffersIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestTwoBuffersIODeterminedSizeReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, bufferingSize = {3}")
	public static Collection<Object[]> parameters() {
		return TestReadableBuffered.generateTestCases(false);
	}
	
	public TestTwoBuffersIODeterminedSizeReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferingSize) {
		super(testFile, testBuf, nbBuf, bufferingSize);
	}
	
	@Override
	protected TwoBuffersIO createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) {
		if (fileSize < bufferingSize)
			return new TwoBuffersIO.DeterminedSize(file, (int)fileSize, 0);
		return new TwoBuffersIO.DeterminedSize(file, bufferingSize, (int)(fileSize - bufferingSize));
	}
	
}
