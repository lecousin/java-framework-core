package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.BufferedIO;

public class TestBufferedIOError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		if (!(io instanceof IO.Readable.Seekable))
			return null;
		return new BufferedIO((IO.Readable.Seekable)io, 2048, 512, 512, true);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		if (!(io instanceof IO.Readable.Seekable))
			return null;
		return new BufferedIO((IO.Readable.Seekable)io, 2048, 512, 512, true);
	}

	@Override
	protected IO.Readable.Seekable getReadableSeekable(IO.Readable.Seekable io) throws Exception {
		return new BufferedIO(io, 2048, 512, 512, true);
	}
	
	@Override
	protected IO.Writable getWritable(IO.Writable io) throws Exception {
		if ((io instanceof IO.Readable.Seekable) && (io instanceof IO.Writable.Seekable) && (io instanceof IO.Resizable))
			return new BufferedIO.ReadWrite((IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable)io, 2048, 512, true);
		return null;
	}

}
