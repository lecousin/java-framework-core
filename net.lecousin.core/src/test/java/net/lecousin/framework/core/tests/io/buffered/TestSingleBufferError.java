package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

public class TestSingleBufferError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		return new SingleBufferReadable(io, 512, false);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		return new SingleBufferReadable(io, 512, false);
	}

	@Override
	protected IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) throws Exception {
		return null;
	}
	
	@Override
	protected IO.Writable getWritable(IO.Writable io) throws Exception {
		return null;
	}

}
