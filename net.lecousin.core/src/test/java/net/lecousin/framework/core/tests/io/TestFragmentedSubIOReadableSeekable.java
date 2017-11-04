package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.io.FragmentedSubIO;
import net.lecousin.framework.io.IO;

@RunWith(Parameterized.class)
public class TestFragmentedSubIOReadableSeekable extends TestReadableSeekable {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestFragmentedSubIOReadableSeekable(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(ReadOnly file, long fileSize) throws Exception {
		return new FragmentedSubIO.Readable(file, f.fragments, true, "fragmented IO");
	}
	
}