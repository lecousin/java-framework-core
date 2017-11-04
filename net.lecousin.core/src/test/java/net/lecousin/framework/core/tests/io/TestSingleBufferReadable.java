package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

@RunWith(Parameterized.class)
public class TestSingleBufferReadable extends TestReadable {

	@Parameters(name = "nbBuf = {2}, useReadFully = {3}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> base = TestIO.UsingGeneratedTestFiles.generateTestCases();
		LinkedList<Object[]> list = new LinkedList<>();
		for (Object[] params : base) {
			Object[] params2 = new Object[params.length + 1];
			System.arraycopy(params, 0, params2, 0, params.length);
			params2[params.length] = Boolean.TRUE;
			list.add(params2);
			params2 = new Object[params.length + 1];
			System.arraycopy(params, 0, params2, 0, params.length);
			params2[params.length] = Boolean.FALSE;
			list.add(params2);
		}
		return list;
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
