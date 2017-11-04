package net.lecousin.framework.core.tests.io;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritableWrapper;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;

@RunWith(Parameterized.class)
public class TestSimpleBufferedWritableWrapper extends TestWritableWrapper {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestSimpleBufferedWritableWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@Override
	protected SimpleBufferedWritable openWritableWrapper(IO.Writable output) {
		return new SimpleBufferedWritable(output, 4096);
	}
	
}
