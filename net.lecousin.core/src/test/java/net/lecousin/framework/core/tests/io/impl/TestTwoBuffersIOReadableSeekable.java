package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.TwoBuffersIO;

@RunWith(Parameterized.class)
public class TestTwoBuffersIOReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestTwoBuffersIOReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected TwoBuffersIO createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) {
		return new TwoBuffersIO(file, 40000, (int)(fileSize - 40000));
	}

}
