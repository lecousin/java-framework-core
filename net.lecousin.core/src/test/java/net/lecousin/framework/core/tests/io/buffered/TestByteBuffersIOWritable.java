package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteBuffersIO;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestByteBuffersIOWritable extends TestWritable {
	
	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestByteBuffersIOWritable(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	private File file;
	
	@Override
	protected IO.Writable createWritable() throws IOException {
		this.file = createFile();
		return new ByteBuffersIO(true, "Test ByteBuffersIO as Writable", Task.Priority.NORMAL);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void flush(IO.Writable io) throws Exception {
		ByteBuffersIO bio = (ByteBuffersIO)io;
		bio.seekSync(SeekType.FROM_BEGINNING, 0);
		FileIO.WriteOnly out = new FileIO.WriteOnly(file, Task.Priority.NORMAL);
		try {
			IOUtil.copy(bio, out, bio.getSizeSync(), false, null, 0).blockResult(0);
		} catch (CancelException e) {
			throw new IOException("Copy ByteBuffersIO to file cancelled", e);
		}
		out.close();
		if (nbBuf == 0)
			Assert.assertArrayEquals(new byte[0], bio.createSingleByteArray());
		else if (nbBuf == 1)
			Assert.assertArrayEquals(testBuf, bio.createSingleByteArray());
	}
	
	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, 0);
	}
	
}
