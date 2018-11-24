package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBuffered;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

@RunWith(Parameterized.class)
public class TestByteArrayIOWritableBuffered extends TestWritableBuffered {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(false);
	}
	
	public TestByteArrayIOWritableBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@Override
	protected IO.Writable.Buffered createWritableBuffered() throws IOException {
		this.file = createFile();
		ByteArrayIO io = new ByteArrayIO(16, "test");
		io.toByteBuffer();
		Assert.assertEquals(16, io.getCapacity());
		return io;
	}

	@Override
	protected void flush(IO.Writable io) throws IOException {
		ByteArrayIO bio = (ByteArrayIO)io;
		FileOutputStream out = new FileOutputStream(file);
		out.write(bio.getArray(), 0, (int)bio.getSizeSync());
		out.close();
	}
	
	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, 0);
	}
	
}
