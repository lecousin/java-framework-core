package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Readable.Buffered;
import net.lecousin.framework.io.IO.Readable.Seekable;
import net.lecousin.framework.io.buffering.PreBufferedReadable;

public class TestPreBufferedReadableError extends TestIOError {

	@Override
	protected Readable getReadable(IOErrorAlways io) throws Exception {
		return new PreBufferedReadable(io, 512, Task.PRIORITY_NORMAL, 512, Task.PRIORITY_NORMAL, 10);
	}

	@Override
	protected Buffered getReadableBuffered(IOErrorAlways io) throws Exception {
		return new PreBufferedReadable(io, 512, Task.PRIORITY_NORMAL, 512, Task.PRIORITY_NORMAL, 10);
	}

	@Override
	protected Seekable getReadableSeekable(IOErrorAlways io) throws Exception {
		return null;
	}

}
