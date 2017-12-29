package net.lecousin.framework.core.tests.io;

import java.io.OutputStream;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestOutputStream;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOAsOutputStream;
import net.lecousin.framework.io.buffering.MemoryIO;

@RunWith(Parameterized.class)
public class TestIOAsOutputStream extends TestOutputStream {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}

	public TestIOAsOutputStream(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected OutputStream createStream() {
		MemoryIO io = new MemoryIO(512, "test");
		return IOAsOutputStream.get(io);
	}
	
	@Override
	protected IO.Readable openReadable(OutputStream out) {
		MemoryIO io = (MemoryIO)((IOAsOutputStream)out).getWrappedIO();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
}
