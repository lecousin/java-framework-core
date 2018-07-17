package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.TwoBuffersIO;

@RunWith(Parameterized.class)
public class TestTwoBuffersIOReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}, test: {3}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = TestIO.UsingGeneratedTestFiles.generateTestCases(false);
		LinkedList<Object[]> list = new LinkedList<>();
		for (Object[] params : base) {
			list.add(createParams(params, 0));
			list.add(createParams(params, 1));
			list.add(createParams(params, 2));
		}
		return list;
	}
	private static Object[] createParams(Object[] original, int test) {
		return new Object[] { original[0], original[1], original[2], Integer.valueOf(test) };
	}
	
	public TestTwoBuffersIOReadableSeekable(File testFile, byte[] testBuf, int nbBuf, int test) {
		super(testFile, testBuf, nbBuf);
		this.test = test;
	}
	
	protected int test;
	
	@Override
	protected TwoBuffersIO createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) {
		if (fileSize == 0)
			return new TwoBuffersIO(file, 1024, 0);
		switch (test) {
		default:
		case 0: return new TwoBuffersIO(file, 40000, (int)(fileSize - 40000));
		case 1: return new TwoBuffersIO(file, 12, (int)(fileSize - 12));
		case 2: return new TwoBuffersIO(file, (int)(fileSize - 12), 12);
		}
	}

}
