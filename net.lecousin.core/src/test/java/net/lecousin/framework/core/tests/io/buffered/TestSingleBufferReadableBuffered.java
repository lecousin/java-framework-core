package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

@RunWith(Parameterized.class)
public class TestSingleBufferReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, useReadFully = {3}, bufferingSize = {4}")
	public static Collection<Object[]> parameters() {
		return addBufferingSize(TestSingleBufferReadable.parameters());
	}
	
	public TestSingleBufferReadableBuffered(File testFile, byte[] testBuf, int nbBuf, boolean useReadFully, int bufferingSize) {
		super(testFile, testBuf, nbBuf, bufferingSize);
		this.useReadFully = useReadFully;
	}
	
	private boolean useReadFully;
	
	@Override
	protected SingleBufferReadable createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) {
		return new SingleBufferReadable(file, bufferingSize, useReadFully);
	}
	
}
