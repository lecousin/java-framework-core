package net.lecousin.framework.core.tests.io.buffered;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.io.buffering.ByteArrayIO;

@RunWith(Parameterized.class)
public class TestByteArrayIOReadWrite extends TestReadWrite {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestByteArrayIOReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected ByteArrayIO openReadWrite() {
		return new ByteArrayIO("test");
	}
	
}
