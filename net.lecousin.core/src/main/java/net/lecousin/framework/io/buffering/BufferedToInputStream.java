package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.io.InputStream;

import net.lecousin.framework.io.IO;

/** Implements an InputStream from an IO.Readable.Buffered. */
public class BufferedToInputStream extends InputStream {

	/** Constructor. */
	public BufferedToInputStream(IO.Readable.Buffered io) {
		this.io = io;
	}
	
	private IO.Readable.Buffered io;
	
	@Override
	public int read() throws IOException {
		return io.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int nb = io.read(b, off, len);
		if (nb == 0 && len > 0) return -1;
		return nb;
	}
	
	@Override
	public void close() throws IOException {
		io.close();
	}
	
	@Override
	public long skip(long n) throws IOException {
		return io.skipSync(n);
	}
	
}
