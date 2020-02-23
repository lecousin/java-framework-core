package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
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
		OutputToInputBuffers o2i = new OutputToInputBuffers(true, 1, Task.Priority.NORMAL);
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
