package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;

@RunWith(Parameterized.class)
public class TestSubIOReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, nbBufSkippedStart = {3}, nbBufSkippedEnd = {4}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = generateTestCases(false);
		LinkedList<Object[]> list = new LinkedList<>();
		for (Object[] params : base) {
			list.add(createParams(params, 0, 0));
			int nbBuf = ((Integer)params[2]).intValue();
			if (nbBuf < 80) continue;
			if (nbBuf <= 100)
				list.add(createParams(params, 60, 15));
			else if (nbBuf <= 1000)
				list.add(createParams(params, 700, 50));
			else if (nbBuf <= 60000)
				list.add(createParams(params, 40000, 5000));
			else if (nbBuf <= 100000)
				list.add(createParams(params, 80000, 5000));
			else
				list.add(createParams(params, nbBuf - 50000, 20000));
		}
		return list;
	}
	private static Object[] createParams(Object[] original, int skipStart, int skipEnd) {
		return new Object[] { original[0], original[1], original[2], Integer.valueOf(skipStart), Integer.valueOf(skipEnd) };
	}
	
	public TestSubIOReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int nbBufSkippedStart, int nbBufSkippedEnd) {
		super(testFile, testBuf, nbBuf - nbBufSkippedStart - nbBufSkippedEnd, 0);
		this.nbBufSkippedStart = nbBufSkippedStart;
	}


	private int nbBufSkippedStart;
	
	@Override
	protected Seekable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws IOException {
		BufferedIO buffered = new BufferedIO(file, file.getSizeSync(), 8192, 8192, false);
		return new SubIO.Readable.Seekable.Buffered(buffered, (long)nbBufSkippedStart * testBuf.length, fileSize, "test subio", true);
	}
	
}
