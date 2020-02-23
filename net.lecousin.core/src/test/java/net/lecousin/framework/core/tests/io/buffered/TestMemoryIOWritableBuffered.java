package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.MemoryIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestMemoryIOWritableBuffered extends TestWritableBuffered {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(false);
	}
	
	public TestMemoryIOWritableBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@Override
	protected IO.Writable.Buffered createWritableBuffered() throws IOException {
		this.file = createFile();
		return new MemoryIO(4096, "test");
	}
	
	@Override
	protected void flush(IO.Writable io) throws Exception {
		MemoryIO mio = (MemoryIO)io;
		FileIO.WriteOnly fio = new FileIO.WriteOnly(file, Task.Priority.NORMAL);
		mio.writeAsyncTo(fio).blockException(0);
		fio.close();
	}
	
	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, 0);
	}
	
}
