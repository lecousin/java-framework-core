package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestOutputToInput;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.out2in.OutputToInput;

@RunWith(Parameterized.class)
public class TestOutputToInputWithFileIO extends TestOutputToInput {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestOutputToInputWithFileIO(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected IO.OutputToInput createOutputToInput() throws IOException {
		File file = File.createTempFile("test", "outputtoinput");
		file.deleteOnExit();
		return new OutputToInput(new FileIO.ReadWrite(file, Task.PRIORITY_NORMAL), "test");
	}
	
}
