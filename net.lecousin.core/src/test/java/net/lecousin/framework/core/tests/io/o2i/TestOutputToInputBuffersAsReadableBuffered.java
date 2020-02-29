package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInputBuffersAsReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestOutputToInputBuffersAsReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf, 0);
	}
	
	@Override
	protected OutputToInputBuffers createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		OutputToInputBuffers o2i = new OutputToInputBuffers(true, nbBuf > 100 ? 3 : 0, Task.Priority.NORMAL);
		Task.cpu("Copy to o2i", Priority.NORMAL, t -> {
			IOUtil.copy(file, o2i, fileSize, false, null, 0).onDone(() -> {
				o2i.endOfData();
				file.closeAsync();
			});
			return null;
		}).executeIn(1000).start();
		return o2i;
	}
	
}
