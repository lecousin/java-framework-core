package net.lecousin.framework.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.lecousin.framework.io.buffering.BufferedToInputStream;

/** Convert an IO into an InputStream.
 * An instance can be obtain using the static get method.
 * This is to avoid having an InputStream wrapped into an IOFromInputStream, wrapped into an IOAsInputStream.
 */
public class IOAsInputStream extends InputStream {

	/** Get an instance for the given IO. */
	public static InputStream get(IO.Readable io) {
		if (io instanceof IOFromInputStream)
			return ((IOFromInputStream)io).getInputStream();
		if (io instanceof IO.Readable.Buffered)
			return new BufferedToInputStream((IO.Readable.Buffered)io);
		return new IOAsInputStream(io);
	}
	
	private IOAsInputStream(IO.Readable io) {
		this.io = io;
	}
	
	private IO.Readable io;
	
	@Override
	public void close() throws IOException {
		io.close();
	}
	
	@Override
	public int available() {
		return 1;
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@SuppressWarnings("sync-override")
	@Override
	public void mark(int readlimit) {
	}
	
	@SuppressWarnings("sync-override")
	@Override
	public void reset() {
	}
	
	private byte[] b = new byte[1];
	private ByteBuffer bb = ByteBuffer.wrap(b);
	
	@Override
	public int read() throws IOException {
		bb.clear();
		if (io.readSync(bb) <= 0) return -1;
		return b[0] & 0xFF;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
		int nb = io.readSync(buffer);
		if (nb == 0 && len > 0) return -1;
		return nb;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public long skip(long n) throws IOException {
		return io.skipSync(n);
	}
	
}
