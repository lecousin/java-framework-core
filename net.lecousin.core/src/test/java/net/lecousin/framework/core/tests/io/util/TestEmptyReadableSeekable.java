package net.lecousin.framework.core.tests.io.util;

import java.io.File;

import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.util.EmptyReadable;

public class TestEmptyReadableSeekable extends TestReadableSeekable {

	public TestEmptyReadableSeekable() {
		super((File)generateTestCases(true).get(0)[0], (byte[])generateTestCases(true).get(0)[1], 0);
	}

	@Override
	protected EmptyReadable createReadableSeekableFromFile(ReadOnly file, long fileSize) {
		file.closeAsync();
		return new EmptyReadable("test empty", file.getPriority());
	}
	
}
