package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.SubIO;

@RunWith(Parameterized.class)
public class TestSubIOReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}, nbBufSkippedEnd = {3}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = generateTestCases();
		LinkedList<Object[]> list = new LinkedList<>();
		for (Object[] params : base) {
			list.add(createParams(params, 0));
			int nbBuf = ((Integer)params[2]).intValue();
			if (nbBuf < 10) continue;
			if (nbBuf < 80)
				list.add(createParams(params, 3));
			else if (nbBuf <= 100)
				list.add(createParams(params, 60));
			else if (nbBuf <= 1000)
				list.add(createParams(params, 700));
			else if (nbBuf <= 100000)
				list.add(createParams(params, 80000));
			else
				list.add(createParams(params, nbBuf - 50000));
		}
		return list;
	}
	private static Object[] createParams(Object[] original, int skipBuf) {
		return new Object[] { original[0], original[1], original[2], Integer.valueOf(skipBuf) };
	}
	
	public TestSubIOReadable(File testFile, byte[] testBuf, int nbBuf, int nbBufSkippedEnd) {
		super(testFile, testBuf, nbBuf - nbBufSkippedEnd);
	}

	@Override
	protected SubIO.Readable createReadableFromFile(ReadOnly file, long fileSize) {
		return new SubIO.Readable(file, fileSize, "test subio", true);
	}
	
}
