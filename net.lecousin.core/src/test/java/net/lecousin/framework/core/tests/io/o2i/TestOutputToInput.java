package net.lecousin.framework.core.tests.io.o2i;

import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;
import net.lecousin.framework.io.out2in.OutputToInput;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInput extends net.lecousin.framework.core.test.io.TestOutputToInput {

	@Parameters(name = "nbBuf = {1}, testCase = {2}")
	public static Collection<Object[]> parameters() {
		return addTestParameter(TestIO.UsingTestData.generateTestCases(true), Integer.valueOf(1), Integer.valueOf(2));
	}
	
	public TestOutputToInput(byte[] testBuf, int nbBuf, int testCase) {
		super(testBuf, nbBuf);
		this.testCase = testCase;
	}
	
	private int testCase;

	@Override
	protected IO.OutputToInput createOutputToInput() {
		switch (testCase) {
		default:
		case 1: return new OutputToInput(new IOInMemoryOrFile(4096, Priority.NORMAL, "test"), "test");
		case 2: return new OutputToInput(new ByteArrayIO(new byte[nbBuf * testBuf.length + 1024], "test"), "test");
		}
	}
	
}
