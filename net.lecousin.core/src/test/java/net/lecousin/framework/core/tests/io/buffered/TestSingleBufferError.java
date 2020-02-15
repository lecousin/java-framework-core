package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.buffering.SingleBufferReadable;

public class TestSingleBufferError extends TestIOError {

	@Override
	protected Readable getReadable(IOErrorAlways io) throws Exception {
		return new SingleBufferReadable(io, 512, false);
	}

	@Override
	protected Buffered getReadableBuffered(IOErrorAlways io) throws Exception {
		return new SingleBufferReadable(io, 512, false);
	}

	@Override
	protected Seekable getReadableSeekable(IOErrorAlways io) throws Exception {
		return null;
	}

}
