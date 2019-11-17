package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestSingleBufferReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}, useReadFully = {3}")
	public static Collection<Object[]> parameters() {
		return addTestParameter(TestIO.UsingGeneratedTestFiles.generateTestCases(true), Boolean.FALSE, Boolean.TRUE);
	}
	
	public TestSingleBufferReadable(File testFile, byte[] testBuf, int nbBuf, boolean useReadFully) {
		super(testFile, testBuf, nbBuf);
		this.useReadFully = useReadFully;
	}
	
	private boolean useReadFully;
	
	@Override
	protected SingleBufferReadable createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		return new SingleBufferReadable(file, 4096, useReadFully);
	}
	
}
