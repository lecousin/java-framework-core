package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBufferedToFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.MemoryIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestMemoryIOWritableBufferedToFile extends TestWritableBufferedToFile {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(false);
	}
	
	public TestMemoryIOWritableBufferedToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@Override
	protected IO.Writable.Buffered createWritableBufferedFromFile(File file) {
		this.file = file;
		return new MemoryIO(4096, "test");
	}
	
	@Override
	protected void flush(IO.Writable.Buffered io) throws Exception {
		MemoryIO mio = (MemoryIO)io;
		FileIO.WriteOnly fio = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		mio.writeAsyncTo(fio).blockException(0);
		fio.close();
	}
	
}