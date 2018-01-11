package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.math.RangeLong;

@RunWith(Parameterized.class)
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
		BufferedIO.ReadOnly buffered = new BufferedIO.ReadOnly(file, 32768, f.realSize);
		IO.Readable.Seekable[] ios = new IO.Readable.Seekable[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = new SubIO.Readable.Seekable(buffered, fragment.min, fragment.getLength(), "fragment " + i, false);
		LinkedIO.Readable.Seekable res = new LinkedIO.Readable.Seekable("linked IO", ios);
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
