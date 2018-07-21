package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.math.RangeLong;

@RunWith(Parameterized.class)
public class TestLinkedIOWithSubIOReadableBuffered2 extends TestReadableBuffered {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadableBuffered2(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	
	@SuppressWarnings("resource")
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		BufferedIO bio = new BufferedIO(file, file.getSizeSync(), 4096, 4096, true);
		IO.Readable.Buffered[] ios = new IO.Readable.Buffered[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = 
				new PreBufferedReadable(
					new SubIO.Readable.Seekable(bio, fragment.min, fragment.getLength(), "fragment " + i, false),
					fragment.getLength(), 512, Task.PRIORITY_NORMAL, 4096, Task.PRIORITY_NORMAL, 5);
		return new LinkedIO.Readable.Buffered.DeterminedSize("linked IO", ios);
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}

}