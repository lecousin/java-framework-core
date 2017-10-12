package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestWritableToFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteBuffersIO;

@RunWith(Parameterized.class)
public class TestByteBuffersIOWritable extends TestWritableToFile {
	
	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestByteBuffersIOWritable(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	private File file;
	
	@Override
	protected IO.Writable createWritableFromFile(File file) throws IOException {
		this.file = file;
		return new ByteBuffersIO(true, "Test ByteBuffersIO as Writable", Task.PRIORITY_NORMAL);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void flush(IO.Writable io) throws IOException {
		ByteBuffersIO bio = (ByteBuffersIO)io;
		bio.seekSync(SeekType.FROM_BEGINNING, 0);
		FileIO.WriteOnly out = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		try {
			IOUtil.copy(bio, out, bio.getSizeSync(), false, null, 0).blockResult(0);
		} catch (CancelException e) {
			throw new IOException("Copy ByteBuffersIO to file cancelled", e);
		}
		out.close();
	}
	
}
