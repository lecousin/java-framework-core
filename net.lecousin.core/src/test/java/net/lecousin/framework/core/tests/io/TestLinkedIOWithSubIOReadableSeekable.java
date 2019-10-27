package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.math.RangeLong;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestLinkedIOWithSubIOReadableSeekable extends TestReadableSeekable {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadableSeekable(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		// this test may be very slow, let's add a buffered layer
		BufferedIO buffered = new BufferedIO(file, f.realSize, 32768, 32768, false);
		IO.Readable.Seekable[] ios = new IO.Readable.Seekable[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = new SubIO.Readable.Seekable(buffered, fragment.min, fragment.getLength(), "fragment " + i, false);
		LinkedIO.Readable.Seekable res = new LinkedIO.Readable.Seekable.DeterminedSize("linked IO", ios);
		res.addCloseListener(() -> {
			buffered.closeAsync();
		});
		return res;
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}
}
