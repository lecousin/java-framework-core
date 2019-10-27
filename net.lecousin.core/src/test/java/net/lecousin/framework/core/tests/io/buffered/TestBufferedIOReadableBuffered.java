package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestBufferedIOReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, bufferingSize = {3}")
	public static Collection<Object[]> parameters() {
		return TestReadableBuffered.generateTestCases(false);
	}
	
	public TestBufferedIOReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferingSize) {
		super(testFile, testBuf, nbBuf, bufferingSize);
	}
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		return new BufferedIO(file, fileSize, bufferingSize / 2, bufferingSize, true);
	}
	
}
