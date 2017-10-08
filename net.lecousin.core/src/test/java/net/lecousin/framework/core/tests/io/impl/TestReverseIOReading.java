package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedReverseIOReading;

@RunWith(Parameterized.class)
public class TestReverseIOReading extends TestIO.UsingGeneratedTestFiles {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestReverseIOReading(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	
	@Override
	protected IO getIOForCommonTests() {
		return new BufferedReverseIOReading(openFile(), 512);
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReverseIO() throws Exception {
		BufferedReverseIOReading rio = new BufferedReverseIOReading(openFile(), 512);
		// backward
		for (int i = nbBuf-1; i >= 0; --i) {
			for (int j = testBuf.length-1; j >= 0; --j) {
				int c;
				try { c = rio.readReverse(); }
				catch (Throwable t) {
					throw new Exception("Error at "+(i*testBuf.length+j), t);
				}
				if (c != (testBuf[j]&0xFF))
					throw new Exception("Invalid character "+c+" ("+(char)c+") at "+(i*testBuf.length+j));
			}
		}
		// forward
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j) {
				int c;
				try { c = rio.read(); }
				catch (Throwable t) {
					throw new Exception("Error at "+(i*testBuf.length+j), t);
				}
				if (c != (testBuf[j]&0xFF))
					throw new Exception("Invalid character "+c+" ("+(char)c+") at "+(i*testBuf.length+j));
			}
		}
		rio.close();
	}
	
}
