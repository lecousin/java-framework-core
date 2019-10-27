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
import net.lecousin.framework.io.util.BroadcastIO;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestBroadcastIO extends TestWritable {

	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingTestData.generateTestCases(true);
	}
	
	public TestBroadcastIO(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	private File f1, f2, f3;
	
	@Override
	protected IO.Writable createWritable() throws IOException {
		f1 = createFile();
		f2 = createFile();
		f3 = createFile();
		FileIO.WriteOnly io1 = new FileIO.WriteOnly(f1, Task.PRIORITY_NORMAL);
		FileIO.WriteOnly io2 = new FileIO.WriteOnly(f2, Task.PRIORITY_NORMAL);
		FileIO.WriteOnly io3 = new FileIO.WriteOnly(f3, Task.PRIORITY_NORMAL);
		return new BroadcastIO(new IO.Writable[] { io1,  io2,  io3 }, Task.PRIORITY_NORMAL, true);
	}
	
	@Override
	protected void check() throws Exception {
		checkFile(f1, testBuf, nbBuf, 0);
		checkFile(f2, testBuf, nbBuf, 0);
		checkFile(f3, testBuf, nbBuf, 0);
	}
	
}
