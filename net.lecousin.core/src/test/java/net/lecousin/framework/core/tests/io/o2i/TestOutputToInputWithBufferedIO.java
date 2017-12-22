package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputToInput;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.out2in.OutputToInput;

@RunWith(Parameterized.class)
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
		FileIO.ReadWrite fio = new FileIO.ReadWrite(file, Task.PRIORITY_NORMAL);
		BufferedIO.ReadWrite bio = new BufferedIO.ReadWrite(fio, 4096, 0);
		return new OutputToInput(bio, "test");
	}
	
}
