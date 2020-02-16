package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.buffering.ReadableToSeekable;

public class TestReadableToSeekableError extends TestIOError {

	@Override
	protected Readable getReadable(ReadableAlwaysError io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}

	@Override
	protected Buffered getReadableBuffered(ReadableAlwaysError io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}

	@Override
	protected Seekable getReadableSeekable(ReadableAlwaysError io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}

}
