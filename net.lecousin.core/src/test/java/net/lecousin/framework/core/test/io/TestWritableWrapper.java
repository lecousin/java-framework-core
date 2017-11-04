package net.lecousin.framework.core.test.io;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Writable;

public abstract class TestWritableWrapper extends TestWritableToFile {
	
	protected TestWritableWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable openWritableWrapper(IO.Writable output) throws Exception;
	
	@Override
	protected Writable createWritableFromFile(File file) {
		return new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
	}

}
