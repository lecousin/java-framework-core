package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.util.NonBufferedReadableIOAsBuffered;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestNonBufferedReadableIOAsBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>();
		Collection<Object[]> tests = TestIO.UsingGeneratedTestFiles.generateTestCases(true);
		for (Object[] t : tests)
			if (((Integer)t[2]).intValue() < 1000)
				list.add(t);
		return list;
	}
	
	public TestNonBufferedReadableIOAsBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) {
		return new NonBufferedReadableIOAsBuffered(file);
	}
	
}
