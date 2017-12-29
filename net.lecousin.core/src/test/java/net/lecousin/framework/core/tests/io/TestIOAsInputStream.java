package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestInputStream;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;

@RunWith(Parameterized.class)
public class TestIOAsInputStream extends TestInputStream {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestIOAsInputStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected InputStream openStream() {
		return IOAsInputStream.get(new FileIO.ReadOnly(testFile, Task.PRIORITY_NORMAL));
	}
	
	@Override
	protected IO getIOForCommonTests() {
		return new FileIO.ReadOnly(testFile, Task.PRIORITY_NORMAL);
	}
	
}
