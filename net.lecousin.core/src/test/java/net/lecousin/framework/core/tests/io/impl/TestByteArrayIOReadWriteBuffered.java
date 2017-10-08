package net.lecousin.framework.core.tests.io.impl;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.core.tests.io.TestReadWriteBuffered;
import net.lecousin.framework.io.buffering.ByteArrayIO;

@RunWith(Parameterized.class)
public class TestByteArrayIOReadWriteBuffered extends TestReadWriteBuffered {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases();
	}
	
	public TestByteArrayIOReadWriteBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected ByteArrayIO openReadWriteBuffered() {
		return new ByteArrayIO("test");
	}
	
}
