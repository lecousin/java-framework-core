package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadWrite;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.FragmentedSubIO;

@RunWith(Parameterized.class)
public class TestFragmentedSubIOReadWrite extends TestReadWrite {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestFragmentedSubIOReadWrite(FragmentedFile f) {
		super(f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	@SuppressWarnings("unchecked")
	@Override
	protected FragmentedSubIO.ReadWrite openReadWrite() throws Exception {
		File tmpFile = File.createTempFile("test", "fragmentedsubio.rw");
		tmpFile.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL);
		fio.setSizeSync(f.file.length());
		return new FragmentedSubIO.ReadWrite(fio, f.fragments, true, "fragmented IO");
	}
	
}
