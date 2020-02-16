package net.lecousin.framework.core.tests.io;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;

public class TestLinkedIOError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		return new LinkedIO.Readable("test error", io);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		if (!(io instanceof IO.Readable.Buffered))
			return null;
		return new LinkedIO.Readable.Buffered("test error", (IO.Readable.Buffered)io);
	}

	@Override
	protected IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) throws Exception {
		return new LinkedIO.Readable.Seekable("test error", io);
	}
	
	@Override
	protected IO.Writable getWritable(IO.Writable io) throws Exception {
		if ((io instanceof IO.Readable.Seekable) && (io instanceof IO.Writable.Seekable))
			return new LinkedIO.ReadWrite("test error", (IO.Readable.Seekable & IO.Writable.Seekable)io);
		return null;
	}

}
