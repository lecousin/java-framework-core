package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.util.NonBufferedReadableIOAsBuffered;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
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
		super(testFile, testBuf, nbBuf, 0);
	}
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) {
		return new NonBufferedReadableIOAsBuffered(file);
	}
	
}
