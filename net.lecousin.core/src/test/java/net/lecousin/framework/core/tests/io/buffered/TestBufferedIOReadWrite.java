package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.BufferedIO;

@RunWith(Parameterized.class)
public class TestBufferedIOReadWrite extends TestReadWrite {

	@Parameters(name = "nbBuf = {1}, small memory = {2}")
	public static Collection<Object[]> parameters() {
		List<Object[]> tests = TestIO.UsingTestData.generateTestCases(false);
		List<Object[]> params = new ArrayList<>(tests.size() * 2);
		for (Object[] o : tests) {
			Object[] p;
			p = new Object[3];
			p[0] = o[0];
			p[1] = o[1];
			p[2] = Boolean.FALSE;
			params.add(p);
			p = new Object[3];
			p[0] = o[0];
			p[1] = o[1];
			p[2] = Boolean.TRUE;
			params.add(p);
		}
		return params;
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
	}
	
	@After
	public void resetMemory() {
		BufferedIO.MemoryManagement.setMemoryLimits(BufferedIO.MemoryManagement.DEFAULT_MEMORY_THRESHOLD, BufferedIO.MemoryManagement.DEFAULT_MAX_MEMORY, BufferedIO.MemoryManagement.DEFAULT_TO_BE_WRITTEN_THRESHOLD);
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	protected BufferedIO.ReadWrite openReadWrite() throws Exception {
		File tmpFile = File.createTempFile("test", "bufferedio.rw");
		tmpFile.deleteOnExit();
		return new BufferedIO.ReadWrite(new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL), 0, 4096, 4096, false);
	}
	
}
