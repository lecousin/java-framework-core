package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.util.ReadableWithProgress;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.progress.WorkProgressImpl;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestReadableWithProgress extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestReadableWithProgress(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected ReadableWithProgress createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		byte[] b = new byte[(int)fileSize];
		if (file.readFullySync(ByteBuffer.wrap(b)) != fileSize)
			throw new Exception("Error loading file into memory");
		file.close();
		ByteArrayIO io = new ByteArrayIO(b, b.length, "Test");
		WorkProgress progress = new WorkProgressImpl(10000, "Test");
		ReadableWithProgress io2 = new ReadableWithProgress(io, fileSize, progress, 10000);
		return io2;
	}
	
}
