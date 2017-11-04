package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.TwoBuffersIO;

@RunWith(Parameterized.class)
public class TestTwoBuffersIOReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestTwoBuffersIOReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected TwoBuffersIO createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		int second = (int)(fileSize - 40000);
		if (second < 0)
			if (fileSize == 0) second = 0; else second = 10000; // for better test coverage
		return new TwoBuffersIO(file, 40000, second);
	}
	
}
