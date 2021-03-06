package net.lecousin.framework.core.tests.io.o2i;

import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputToInput;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInputBuffers extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestOutputToInputBuffers(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@Override
	protected IO.OutputToInput createOutputToInput() {
		return new OutputToInputBuffers(true, Task.Priority.NORMAL);
	}
	
}
