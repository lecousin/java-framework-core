package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOFromInputStream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestIOFromInputStream extends TestReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestIOFromInputStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected IOFromInputStream createReadableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		File f = file.getFile();
		file.close();
		FileInputStream in = new FileInputStream(f);
		return new IOFromInputStream(in, f.getAbsolutePath(), Threading.getDrivesManager().getTaskManager(f), Task.Priority.NORMAL);
	}
	
}
