package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.SubIO;

public class TestSubIOError extends TestIOError {

	@Override
	protected Readable getReadable(IOError1 io) {
		return new SubIO.Readable(io, 4096, "test error", true);
	}

	@Override
	protected Buffered getReadableBuffered(IOError1 io) {
		return new SubIO.Readable.Seekable.Buffered(io, 10, 4096, "test error", true);
	}

	@Override
	protected Seekable getReadableSeekable(IOError1 io) {
		return new SubIO.Readable.Seekable(io, 10, 4096, "test error", true);
	}

}
