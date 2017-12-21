package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;

@RunWith(Parameterized.class)
public class TestSimpleBufferedReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestSimpleBufferedReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected SimpleBufferedReadable createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		return new SimpleBufferedReadable(file, 4096);
	}
	
}
