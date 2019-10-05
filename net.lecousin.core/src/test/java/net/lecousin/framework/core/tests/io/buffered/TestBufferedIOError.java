package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.buffering.BufferedIO;

public class TestBufferedIOError extends TestIOError {

	@Override
	protected Readable getReadable(IOError1 io) throws Exception {
		return new BufferedIO(io, 512, 512, true);
	}

	@Override
	protected Buffered getReadableBuffered(IOError1 io) throws Exception {
		return new BufferedIO(io, 512, 512, true);
	}

	@Override
	protected Seekable getReadableSeekable(IOError1 io) throws Exception {
		return new BufferedIO(io, 512, 512, true);
	}

}