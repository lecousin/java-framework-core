package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ReadableToSeekable;
import net.lecousin.framework.io.out2in.OutputToInput;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestReadableToSeekableReadableSeekableUsingOutputToInput extends TestReadableSeekable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestReadableToSeekableReadableSeekableUsingOutputToInput(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		File tmp = File.createTempFile("test", "readableasseekable");
		tmp.deleteOnExit();
		FileIO.ReadWrite tmpIO = new FileIO.ReadWrite(tmp, Task.PRIORITY_NORMAL);
		OutputToInput o2i = new OutputToInput(tmpIO, "test");
		IOUtil.copy(file, o2i, fileSize, false, null, 0).listenInline(() -> {
			file.closeAsync();
			o2i.endOfData();
		});
		return new ReadableToSeekable(o2i, 512);
	}
	
}
