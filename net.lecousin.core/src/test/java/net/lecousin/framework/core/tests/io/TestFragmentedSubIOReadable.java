package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestReadable;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.FragmentedSubIO;
import net.lecousin.framework.io.IO;

@RunWith(Parameterized.class)
public class TestFragmentedSubIOReadable extends TestReadable {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestFragmentedSubIOReadable(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	
	@Override
	protected IO.Readable createReadableFromFile(FileIO.ReadOnly file, long fileSize) {
		return new FragmentedSubIO.Readable(file, f.fragments, true, "fragmented IO");
	}
	
}
