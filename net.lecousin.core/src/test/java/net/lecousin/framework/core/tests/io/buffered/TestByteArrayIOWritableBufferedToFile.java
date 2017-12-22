package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBufferedToFile;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

@RunWith(Parameterized.class)
public class TestByteArrayIOWritableBufferedToFile extends TestWritableBufferedToFile {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(false);
	}
	
	public TestByteArrayIOWritableBufferedToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@Override
	protected IO.Writable.Buffered createWritableBufferedFromFile(File file) throws IOException {
		this.file = file;
		return new ByteArrayIO("test");
	}

	@Override
	protected void flush(IO.Writable.Buffered io) throws IOException {
		ByteArrayIO bio = (ByteArrayIO)io;
		FileOutputStream out = new FileOutputStream(file);
		out.write(bio.getArray(), 0, (int)bio.getSizeSync());
		out.close();
	}
	
}
