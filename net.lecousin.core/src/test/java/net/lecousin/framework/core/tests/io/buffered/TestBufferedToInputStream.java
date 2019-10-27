package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestInputStream;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedToInputStream;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;

@RunWith(Parameterized.class)
public class TestBufferedToInputStream extends TestInputStream {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestBufferedToInputStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected InputStream openStream() {
		FileIO.ReadOnly io = new FileIO.ReadOnly(testFile, Task.PRIORITY_NORMAL);
		SimpleBufferedReadable bio = new SimpleBufferedReadable(io, 4096);
		return new BufferedToInputStream(bio, nbBuf == 0);
	}
	
	@Override
	protected IO getIOForCommonTests() {
		Assume.assumeTrue(nbBuf < 5000);
		return new FileIO.ReadOnly(testFile, Task.PRIORITY_NORMAL);
	}
	
}
