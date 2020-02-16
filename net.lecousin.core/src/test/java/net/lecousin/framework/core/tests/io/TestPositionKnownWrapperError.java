package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.PositionKnownWrapper;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;

public class TestPositionKnownWrapperError extends TestIOError {

	@Override
	protected Readable getReadable(ReadableAlwaysError io) throws Exception {
		return new PositionKnownWrapper.Readable(io);
	}

	@Override
	protected Buffered getReadableBuffered(ReadableAlwaysError io) throws Exception {
		return new PositionKnownWrapper.Readable.Buffered(io);
	}

	@Override
	protected Seekable getReadableSeekable(ReadableAlwaysError io) throws Exception {
		return null;
	}

}
