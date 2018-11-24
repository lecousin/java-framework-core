package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestWritable;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromOutputStream;

@RunWith(Parameterized.class)
public class TestIOFromOutputStream extends TestWritable {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestIOFromOutputStream(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	private File file;
	
	@SuppressWarnings("resource")
	@Override
	protected IO.Writable createWritable() throws IOException {
		file = createFile();
		FileOutputStream out = new FileOutputStream(file);
		return new IOFromOutputStream(out, file.getAbsolutePath(), Threading.getDrivesTaskManager().getTaskManager(file), Task.PRIORITY_NORMAL);
	}
	
	@Override
	protected void check() throws Exception {
		checkFile(file, testBuf, nbBuf, 0);
	}
}
