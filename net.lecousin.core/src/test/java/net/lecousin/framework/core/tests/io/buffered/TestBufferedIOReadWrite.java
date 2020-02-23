package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;
import net.lecousin.framework.memory.MemoryManager;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestBufferedIOReadWrite extends TestReadWrite {

	@Parameters(name = "nbBuf = {1}, small memory = {2}")
	public static Collection<Object[]> parameters() {
		return addTestParameter(TestIO.UsingTestData.generateTestCases(false), Boolean.FALSE, Boolean.TRUE);
	}
	
	public TestBufferedIOReadWrite(byte[] testBuf, int nbBuf, boolean smallMemory) {
		super(testBuf, nbBuf);
		this.smallMemory = smallMemory;
	}
	
	private boolean smallMemory;
	
	@Before
	public void initMemory() {
		if (smallMemory)
			BufferedIO.MemoryManagement.setMemoryLimits(48 * 1024, 64 * 1024, 4 * 1024);
		else
			BufferedIO.MemoryManagement.setMemoryLimits(BufferedIO.MemoryManagement.DEFAULT_MEMORY_THRESHOLD, BufferedIO.MemoryManagement.DEFAULT_MAX_MEMORY, BufferedIO.MemoryManagement.DEFAULT_TO_BE_WRITTEN_THRESHOLD);
		BufferedIO.MemoryManagement.getMaxMemory();
		BufferedIO.MemoryManagement.getMemoryThreshold();
		BufferedIO.MemoryManagement.getToBeWrittenThreshold();
	}
	
	@After
	public void resetMemory() {
		BufferedIO.MemoryManagement.setMemoryLimits(BufferedIO.MemoryManagement.DEFAULT_MEMORY_THRESHOLD, BufferedIO.MemoryManagement.DEFAULT_MAX_MEMORY, BufferedIO.MemoryManagement.DEFAULT_TO_BE_WRITTEN_THRESHOLD);
		MemoryManager.freeMemory(FreeMemoryLevel.LOW);
		MemoryManager.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		MemoryManager.freeMemory(FreeMemoryLevel.MEDIUM);
		MemoryManager.freeMemory(FreeMemoryLevel.URGENT);
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	protected BufferedIO.ReadWrite openReadWrite() throws Exception {
		File tmpFile = File.createTempFile("test", "bufferedio.rw");
		tmpFile.deleteOnExit();
		return new BufferedIO.ReadWrite(new FileIO.ReadWrite(tmpFile, Task.Priority.NORMAL), 0, 4096, 4096, false);
	}
	
}
