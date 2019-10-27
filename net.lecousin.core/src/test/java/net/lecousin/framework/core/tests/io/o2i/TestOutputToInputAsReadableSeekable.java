package net.lecousin.framework.core.tests.io.o2i;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputToInput;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.out2in.OutputToInput;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestOutputToInputAsReadableSeekable extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestOutputToInputAsReadableSeekable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		file.close();
		File f = File.createTempFile("test", "outputtoinput");
		f.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(f, Task.PRIORITY_NORMAL);
		BufferedIO.ReadWrite bio = new BufferedIO.ReadWrite(fio, 0, 4096, 4096, false);
		OutputToInput o2i = new OutputToInput(bio, "test");
		TestOutputToInput.writeBg(o2i, nbBuf, testBuf);
		return o2i;
	}
	
}
