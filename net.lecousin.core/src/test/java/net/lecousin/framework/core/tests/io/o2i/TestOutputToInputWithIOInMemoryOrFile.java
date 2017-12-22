package net.lecousin.framework.core.tests.io.o2i;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputToInput;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;
import net.lecousin.framework.io.out2in.OutputToInput;

@RunWith(Parameterized.class)
public class TestOutputToInputWithIOInMemoryOrFile extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestOutputToInputWithIOInMemoryOrFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected IO.OutputToInput createOutputToInput() {
		return new OutputToInput(new IOInMemoryOrFile(1 * 1024 * 1024, Task.PRIORITY_NORMAL, "test"), "test");
	}
	
}
