package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestIOInMemoryOrFileReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestIOInMemoryOrFileReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		((IOInMemoryOrFile)io).getMaxInMemory();
	}
	
	@Override
	protected IOInMemoryOrFile createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		IOInMemoryOrFile io = new IOInMemoryOrFile(2*1024*1024, Task.Priority.NORMAL, "test");
		IOUtil.copy(file, io, fileSize, false, null, 0).blockThrow(0);
		file.close();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
}
