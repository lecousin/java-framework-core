package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.SubIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestSubIOWritable extends TestWritable {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestSubIOWritable(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;

	@Override
	protected IO.Writable createWritable() throws IOException {
		file = createFile();
		FileIO.WriteOnly f = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		f.setSizeSync(testBuf.length * (nbBuf + 100));
		f.seekSync(SeekType.FROM_BEGINNING, testBuf.length * 25);
		return new SubIO.Writable(f, testBuf.length * 25, testBuf.length * nbBuf, "test subio writable", true);
	}

	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, testBuf.length * 25);
	}
}
