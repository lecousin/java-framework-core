package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

@RunWith(Parameterized.class)
public class TestOutputToInputBuffersAsReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestOutputToInputBuffersAsReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected OutputToInputBuffers createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		OutputToInputBuffers o2i = new OutputToInputBuffers(true, -3, Task.PRIORITY_NORMAL);
		IOUtil.copy(file, o2i, fileSize, false, null, 0).blockException(0);
		o2i.endOfData();
		file.closeAsync();
		return o2i;
	}
	
}
