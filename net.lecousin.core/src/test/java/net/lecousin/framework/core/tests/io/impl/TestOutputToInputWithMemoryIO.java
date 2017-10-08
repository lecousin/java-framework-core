package net.lecousin.framework.core.tests.io.impl;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestOutputToInput;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.out2in.OutputToInput;

@RunWith(Parameterized.class)
public class TestOutputToInputWithMemoryIO extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestOutputToInputWithMemoryIO(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected IO.OutputToInput createOutputToInput() {
		return new OutputToInput(new MemoryIO(4096, "test"), "test");
	}
	
}
