package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.out2in.OutputToInput;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInput extends net.lecousin.framework.core.test.io.TestOutputToInput {

	@Parameters(name = "nbBuf = {1}, testCase = {2}")
	public static Collection<Object[]> parameters() {
		return addTestParameter(TestIO.UsingTestData.generateTestCases(true), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(5));
	}
	
	public TestOutputToInput(byte[] testBuf, int nbBuf, int testCase) {
		super(testBuf, nbBuf);
		this.testCase = testCase;
	}
	
	private int testCase;

	@Override
	protected IO.OutputToInput createOutputToInput() throws IOException {
		switch (testCase) {
		default:
		case 1: return new OutputToInput(new IOInMemoryOrFile(1 * 1024 * 1024, Priority.NORMAL, "test"), "test");
		case 2: return new OutputToInput(new ByteArrayIO(new byte[nbBuf * testBuf.length + 1024], "test"), "test");
		case 3: return new OutputToInput(new MemoryIO(4096, "test"), "test");
		case 4: {
			File file = File.createTempFile("test", "outputtoinput");
			file.deleteOnExit();
			FileIO.ReadWrite fio = new FileIO.ReadWrite(file, Task.Priority.NORMAL);
			BufferedIO.ReadWrite bio = new BufferedIO.ReadWrite(fio, 0, 4096, 4096, false);
			return new OutputToInput(bio, "test");
		}
		case 5: {
			File file = File.createTempFile("test", "outputtoinput");
			file.deleteOnExit();
			return new OutputToInput(new FileIO.ReadWrite(file, Task.Priority.NORMAL), "test");
		}
		}
	}
	
}
