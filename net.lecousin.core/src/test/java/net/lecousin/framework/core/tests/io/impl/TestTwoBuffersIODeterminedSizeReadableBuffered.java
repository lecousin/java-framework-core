package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.TwoBuffersIO;

@RunWith(Parameterized.class)
public class TestTwoBuffersIODeterminedSizeReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestTwoBuffersIODeterminedSizeReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected TwoBuffersIO createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) {
		if (fileSize <= 40000)
			return new TwoBuffersIO.DeterminedSize(file, (int)fileSize, 0);
		return new TwoBuffersIO.DeterminedSize(file, 40000, (int)(fileSize - 40000));
	}
	
}
