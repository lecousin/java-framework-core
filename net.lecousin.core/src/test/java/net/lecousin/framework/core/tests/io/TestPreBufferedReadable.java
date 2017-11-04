package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;

@RunWith(Parameterized.class)
public class TestPreBufferedReadable extends TestReadable {

	private static final int[][] bufferSizes = {
		new int[] { 64*1024*1024, 64*1024*1024, 10 }, // very big buffers	
		new int[] { 128*1024, 1024*1024, 10 }, // big buffers	
		new int[] { 4*1024, 16*1024, 10 }, // medium buffers	
		new int[] { 512, 2*1024, 10 }, // small buffers	
		new int[] { 16, 32, 10 }, // tiny buffers	
	};
	
	@Parameters(name = "nbBuf = {2}, firstBufferSize = {3}, nextBufferSize = {4}, nbNextBuffers = {5}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = generateTestCases();
		LinkedList<Object[]> list = new LinkedList<>();
		for (int i = 0; i < bufferSizes.length; ++i)
			for (Object[] params : base) {
				list.add(new Object[] {
					params[0], params[1], params[2],
					Integer.valueOf(bufferSizes[i][0]), Integer.valueOf(bufferSizes[i][1]), Integer.valueOf(bufferSizes[i][2]) 
				});
			}
		return list; 
	}
	
	public TestPreBufferedReadable(File testFile, byte[] testBuf, int nbBuf, int bufferSize1, int bufferSize2, int nbBuffers) {
		super(testFile, testBuf, nbBuf);
		this.bufferSize1 = bufferSize1;
		this.bufferSize2 = bufferSize2;
		this.nbBuffers = nbBuffers;
	}
	
	private int bufferSize1, bufferSize2, nbBuffers;
	
	@Override
	protected IO.Readable.Buffered createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		return new PreBufferedReadable(file, bufferSize1, Task.PRIORITY_IMPORTANT, bufferSize2, Task.PRIORITY_IMPORTANT, nbBuffers);
	}

}
