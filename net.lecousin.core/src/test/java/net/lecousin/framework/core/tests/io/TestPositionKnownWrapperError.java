package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.PositionKnownWrapper;

public class TestPositionKnownWrapperError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		return new PositionKnownWrapper.Readable(io);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		if (io instanceof IO.Readable.Buffered)
			return new PositionKnownWrapper.Readable.Buffered((IO.Readable.Buffered)io);
		return null;
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
