package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.math.RangeLong;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestLinkedIOWithSubIOReadable extends TestReadable {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadable(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	
	@Override
	protected IO.Readable createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		IO.Readable[] ios = new IO.Readable[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = new SubIO.Readable.Seekable(file, fragment.min, fragment.getLength(), "fragment " + i, false);
		LinkedIO.Readable.DeterminedSize io = new LinkedIO.Readable.DeterminedSize("linked IO", ios);
		io.addCloseListener(() -> { try { file.close(); } catch (Exception e) {}});
		return io;
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}
}
