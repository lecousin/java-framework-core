package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.SubIO;

public class TestSubIOError extends TestIOError {

	@Override
	protected Readable getReadable(IO.Readable io) {
		return new SubIO.Readable(io, 4096, "test error", true);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) {
		if ((io instanceof IO.Readable.Seekable) && (io instanceof IO.Readable.Buffered))
			return new SubIO.Readable.Seekable.Buffered((IO.Readable.Seekable & IO.Readable.Buffered)io, 10, 4096, "test error", true);
		return null;
	}

	@Override
	protected IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) {
		return new SubIO.Readable.Seekable(io, 10, 4096, "test error", true);
	}
	
	@Override
	protected IO.Writable getWritable(IO.Writable io) throws Exception {
		return new SubIO.Writable(io, 0, 4096, "test error", true);
	}

}
