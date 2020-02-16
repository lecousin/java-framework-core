package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ReadableToSeekable;

public class TestReadableToSeekableError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}

	@Override
	protected IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) throws Exception {
		return new ReadableToSeekable(io, 512);
	}
	
	@Override
	protected IO.Writable getWritable(IO.Writable io) throws Exception {
		return null;
	}

}
