package net.lecousin.framework.core.tests.io.impl;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestOutputToInput;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;

@RunWith(Parameterized.class)
public class TestOutputToInputBuffers extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestOutputToInputBuffers(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@Override
	protected IO.OutputToInput createOutputToInput() {
		return new OutputToInputBuffers(true, Task.PRIORITY_NORMAL);
	}
	
}
