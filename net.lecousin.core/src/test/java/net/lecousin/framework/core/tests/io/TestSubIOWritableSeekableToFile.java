package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestWritableToFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.SubIO;

@RunWith(Parameterized.class)
public class TestSubIOWritableSeekableToFile extends TestWritableToFile {

	@Parameters(name = "nbBuf = {1}, nbBufSkippedStart = {2}, nbBufSkippedEnd = {3}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = generateTestCases(false);
		LinkedList<Object[]> list = new LinkedList<>();
		for (Object[] params : base) {
			int nbBuf = ((Integer)params[1]).intValue();
			if (nbBuf <= 1000)
				list.add(createParams(params, 0, 0));
			if (nbBuf < 80) continue;
			if (nbBuf <= 100)
				list.add(createParams(params, 60, 15));
			else if (nbBuf <= 1000)
				list.add(createParams(params, 700, 50));
			else if (nbBuf <= 100000)
				list.add(createParams(params, 80000, 5000));
			else
				list.add(createParams(params, nbBuf - 50000, 20000));
		}
		return list;
	}
	private static Object[] createParams(Object[] original, int skipStart, int skipEnd) {
		return new Object[] { original[0], original[1], Integer.valueOf(skipStart), Integer.valueOf(skipEnd) };
	}
	
	public TestSubIOWritableSeekableToFile(byte[] testBuf, int nbBuf, int nbBufSkippedStart, int nbBufSkippedEnd) {
		super(testBuf, nbBuf - nbBufSkippedStart - nbBufSkippedEnd);
		this.nbBufSkippedStart = nbBufSkippedStart;
		this.nbBufSkippedEnd = nbBufSkippedEnd;
	}
	
	protected int nbBufSkippedStart;
	protected int nbBufSkippedEnd;

	@Override
	protected Writable createWritableFromFile(File file) throws IOException {
		FileIO.ReadWrite fio = new FileIO.ReadWrite(file, Task.PRIORITY_NORMAL);
		long size = nbBuf * testBuf.length;
		fio.setSizeSync(size + (nbBufSkippedStart + nbBufSkippedEnd) * testBuf.length);
		return new SubIO.Writable.Seekable(fio, nbBufSkippedStart * testBuf.length, size, "test subio", true);
	}
	
	@Override
	public long getFileOffset() {
		return nbBufSkippedStart * testBuf.length;
	}
	
}
