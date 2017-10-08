package net.lecousin.framework.core.tests.io.impl;

import java.io.File;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.tests.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

@RunWith(Parameterized.class)
public class TestSingleBufferReadableBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}, useReadFully = {3}")
	public static Collection<Object[]> parameters() {
		return TestSingleBufferReadable.parameters();
	}
	
	public TestSingleBufferReadableBuffered(File testFile, byte[] testBuf, int nbBuf, boolean useReadFully) {
		super(testFile, testBuf, nbBuf);
		this.useReadFully = useReadFully;
	}
	
	private boolean useReadFully;
	
	@Override
	protected SingleBufferReadable createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) {
		return new SingleBufferReadable(file, 4096, useReadFully);
	}
	
}
