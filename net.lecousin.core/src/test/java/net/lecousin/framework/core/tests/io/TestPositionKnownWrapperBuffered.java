package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.PositionKnownWrapper;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;

@RunWith(Parameterized.class)
public class TestPositionKnownWrapperBuffered extends TestReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestPositionKnownWrapperBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		File f = file.getFile();
		IOFromInputStream io = new IOFromInputStream(new FileInputStream(f), f.getAbsolutePath(), file.getTaskManager(), Task.PRIORITY_NORMAL);
		file.closeAsync();
		SimpleBufferedReadable bio = new SimpleBufferedReadable(io, 4096);
		return new PositionKnownWrapper.Readable.Buffered(bio);
	}
	
}
