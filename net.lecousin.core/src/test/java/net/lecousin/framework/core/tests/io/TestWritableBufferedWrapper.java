package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Writable.Buffered;

public abstract class TestWritableBufferedWrapper extends TestWritableBufferedToFile {

	protected TestWritableBufferedWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable.Buffered openWritableBufferedWrapper(IO.Writable output) throws IOException;
	
	@SuppressWarnings("resource")
	@Override
	protected Buffered createWritableBufferedFromFile(File file) throws IOException {
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		return openWritableBufferedWrapper(fileIO);
	}
	
}
