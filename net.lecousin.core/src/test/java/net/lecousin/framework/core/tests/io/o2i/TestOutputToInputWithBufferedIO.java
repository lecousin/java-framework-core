package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputToInput;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.out2in.OutputToInput;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInputWithBufferedIO extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestOutputToInputWithBufferedIO(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected IO.OutputToInput createOutputToInput() throws IOException {
		File file = File.createTempFile("test", "outputtoinput");
		file.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(file, Task.Priority.NORMAL);
		BufferedIO.ReadWrite bio = new BufferedIO.ReadWrite(fio, 0, 4096, 4096, false);
		return new OutputToInput(bio, "test");
	}
	
}
