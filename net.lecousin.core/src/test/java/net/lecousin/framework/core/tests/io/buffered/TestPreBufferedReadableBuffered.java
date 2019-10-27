package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestPreBufferedReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, firstBufferSize = {3}, nextBufferSize = {4}, nbNextBuffers = {5}")
	public static Collection<Object[]> parameters() {
		return TestPreBufferedReadable.parameters();
	}
	
	public TestPreBufferedReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferSize1, int bufferSize2, int nbBuffers) {
		super(testFile, testBuf, nbBuf, 0);
		this.bufferSize1 = bufferSize1;
		this.bufferSize2 = bufferSize2;
		this.nbBuffers = nbBuffers;
	}
	
	private int bufferSize1, bufferSize2, nbBuffers;
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		return new PreBufferedReadable(file, bufferSize1, Task.PRIORITY_IMPORTANT, bufferSize2, Task.PRIORITY_IMPORTANT, nbBuffers);
	}

}
