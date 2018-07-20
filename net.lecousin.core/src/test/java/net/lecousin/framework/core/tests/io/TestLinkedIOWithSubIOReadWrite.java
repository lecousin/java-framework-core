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
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.math.RangeLong;

@RunWith(Parameterized.class)
public class TestLinkedIOWithSubIOReadWrite extends TestReadWrite {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadWrite(FragmentedFile f) {
		super(f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	@SuppressWarnings("unchecked")
	@Override
	protected LinkedIO.ReadWrite openReadWrite() throws Exception {
		File tmpFile = File.createTempFile("test", "fragmentedsubio.rw");
		tmpFile.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL);
		fio.setSizeSync(f.file.length());
		SubIO.ReadWrite[] ios = new SubIO.ReadWrite[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = new SubIO.ReadWrite(fio, fragment.min, fragment.getLength(), "fragment " + i, false);
		return new LinkedIO.ReadWrite("linked IO", ios);
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}
}
