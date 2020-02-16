package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;

public class TestPreBufferedReadableError extends TestIOError {

	@Override
	protected IO.Readable getReadable(IO.Readable io) throws Exception {
		return new PreBufferedReadable(io, 512, Task.PRIORITY_NORMAL, 512, Task.PRIORITY_NORMAL, 10);
	}

	@Override
	protected IO.Readable.Buffered getReadableBuffered(IO.Readable io) throws Exception {
		return new PreBufferedReadable(io, 512, Task.PRIORITY_NORMAL, 512, Task.PRIORITY_NORMAL, 10);
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
