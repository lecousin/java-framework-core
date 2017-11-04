package net.lecousin.framework.core.tests.io;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableBufferedWrapper;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;

@RunWith(Parameterized.class)
public class TestSimpleBufferedWritableBufferedWrapper extends TestWritableBufferedWrapper {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestSimpleBufferedWritableBufferedWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	@Override
	protected SimpleBufferedWritable openWritableBufferedWrapper(IO.Writable output) {
		return new SimpleBufferedWritable(output, 4096);
	}
	
}
