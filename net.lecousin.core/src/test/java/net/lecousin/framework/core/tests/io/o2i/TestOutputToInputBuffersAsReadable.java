package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestOutputToInputBuffersAsReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestOutputToInputBuffersAsReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected OutputToInputBuffers createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		OutputToInputBuffers o2i = new OutputToInputBuffers(true, 1, Task.PRIORITY_NORMAL);
		IOUtil.copy(file, o2i, fileSize, false, null, 0).onDone(
			() -> {
				o2i.endOfData();
				file.closeAsync();
			},
			(error) -> {
				o2i.signalErrorBeforeEndOfData(error);
				file.closeAsync();
			},
			(cancel) -> {
				o2i.endOfData();
				file.closeAsync();
			}
		);
		return o2i;
	}
	
}
