package net.lecousin.framework.core.tests.io.util;

import java.io.File;

import net.lecousin.framework.core.test.io.TestReadableBuffered;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.util.EmptyReadable;

public class TestEmptyReadableBuffered extends TestReadableBuffered {

	public TestEmptyReadableBuffered() {
		super((File)generateTestCases(true).get(0)[0], (byte[])generateTestCases(true).get(0)[1], 0);
	}

	@Override
	protected EmptyReadable createReadableBufferedFromFile(ReadOnly file, long fileSize) {
		return new EmptyReadable("test empty", file.getPriority());
	}
	
}
