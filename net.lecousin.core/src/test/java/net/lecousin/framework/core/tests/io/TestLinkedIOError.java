package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.LinkedIO;

public class TestLinkedIOError extends TestIOError {

	@Override
	protected Readable getReadable(ReadableAlwaysError io) throws Exception {
		return new LinkedIO.Readable("test error", io);
	}

	@Override
	protected Buffered getReadableBuffered(ReadableAlwaysError io) throws Exception {
		return new LinkedIO.Readable.Buffered("test error", io);
	}

	@Override
	protected Seekable getReadableSeekable(ReadableAlwaysError io) throws Exception {
		return new LinkedIO.Readable.Seekable("test error", io);
	}

}
