package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestWritableBufferedToFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.MemoryIO;

@RunWith(Parameterized.class)
public class TestMemoryIOWritableBufferedToFile extends TestWritableBufferedToFile {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
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
	protected void flush(IO.Writable.Buffered io) throws IOException {
		MemoryIO mio = (MemoryIO)io;
		FileIO.WriteOnly fio = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		mio.writeAsyncTo(fio).block(0);
		fio.close();
	}
	
}
