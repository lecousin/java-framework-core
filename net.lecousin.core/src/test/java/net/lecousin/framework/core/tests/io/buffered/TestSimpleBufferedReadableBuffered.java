package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;

@RunWith(Parameterized.class)
public class TestSimpleBufferedReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestSimpleBufferedReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected SimpleBufferedReadable createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) {
		return new SimpleBufferedReadable(file, 4096);
	}
	
}
