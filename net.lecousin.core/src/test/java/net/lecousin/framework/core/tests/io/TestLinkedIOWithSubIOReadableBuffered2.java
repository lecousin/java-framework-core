package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.math.RangeLong;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestLinkedIOWithSubIOReadableBuffered2 extends TestReadableBuffered {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadableBuffered2(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf, 0);
		this.f = f;
	}
	
	private FragmentedFile f;

	
	@Override
	protected IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize, int bufferingSize) throws Exception {
		BufferedIO bio = new BufferedIO(file, file.getSizeSync(), 4096, 4096, true);
		IO.Readable.Buffered[] ios = new IO.Readable.Buffered[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = 
				new PreBufferedReadable(
					new SubIO.Readable.Seekable(bio, fragment.min, fragment.getLength(), "fragment " + i, false),
					fragment.getLength(), 512, Task.Priority.NORMAL, 4096, Task.Priority.NORMAL, 5);
		LinkedIO.Readable.Buffered.DeterminedSize io = new LinkedIO.Readable.Buffered.DeterminedSize("linked IO", ios);
		io.addCloseListener(() -> { try { bio.close(); } catch (Exception e) {}});
		return io;
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}

}
