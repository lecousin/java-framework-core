package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputStream;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOAsOutputStream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestIOAsOutputStreamWithFileIO extends TestOutputStream {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}

	public TestIOAsOutputStreamWithFileIO(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected OutputStream createStream() throws Exception {
		File file = File.createTempFile("test", "outputstream");
		file.deleteOnExit();
		FileIO.ReadWrite io = new FileIO.ReadWrite(file, Task.PRIORITY_NORMAL);
		return IOAsOutputStream.get(io);
	}
	
	@Override
	protected IO.Readable openReadable(OutputStream out) throws Exception {
		FileIO.ReadWrite io = (FileIO.ReadWrite)((IOAsOutputStream)out).getWrappedIO();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
}
