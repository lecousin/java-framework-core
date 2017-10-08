package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;

@RunWith(Parameterized.class)
public class TestPreBufferedReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, firstBufferSize = {3}, nextBufferSize = {4}, nbNextBuffers = {5}")
	public static Collection<Object[]> parameters() {
		return TestPreBufferedReadable.parameters();
	}
	
	public TestPreBufferedReadableBuffered(File testFile, byte[] testBuf, int nbBuf, int bufferSize1, int bufferSize2, int nbBuffers) {
		super(testFile, testBuf, nbBuf);
		this.bufferSize1 = bufferSize1;
		this.bufferSize2 = bufferSize2;
		this.nbBuffers = nbBuffers;
	}
	
	private int bufferSize1, bufferSize2, nbBuffers;
	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		return new PreBufferedReadable(file, bufferSize1, Task.PRIORITY_IMPORTANT, bufferSize2, Task.PRIORITY_IMPORTANT, nbBuffers);
	}

}
