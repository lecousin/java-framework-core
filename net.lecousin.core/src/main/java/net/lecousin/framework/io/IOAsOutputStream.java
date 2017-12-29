package net.lecousin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Convert an IO into an OutputStream.
 */
public abstract class IOAsOutputStream extends OutputStream {

	/** Create an OutputStream based on the given writable IO. */
	public static OutputStream get(IO.Writable io) {
		if (io instanceof IOFromOutputStream)
			return ((IOFromOutputStream)io).getOutputStream();
		if (io instanceof IO.WritableByteStream)
			return new ByteStream((IO.WritableByteStream)io);
		return new Writable(io);
	}
	
	public abstract IO.Writable getWrappedIO();
	
	/** Default implementation of IOAsOutputStream for a Writable. */
	public static class Writable extends IOAsOutputStream {
		/** Constructor. */
		public Writable(IO.Writable io) {
			this.io = io;
		}
		
		private IO.Writable io;
		
		@Override
		public IO.Writable getWrappedIO() {
			return io;
		}
		
		@Override
		public void close() throws IOException {
			try { io.close(); }
			catch (Exception e) { throw IO.error(e); }
		}
	
		@Override
		public void write(int b) throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			buffer.put((byte)b);
			buffer.flip();
			io.writeSync(buffer);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
			io.writeSync(buffer);
		}
	}
	
	/** Implementation of IOAsOutputStream for a WritableByteStream. */
	public static class ByteStream extends IOAsOutputStream {
		/** Constructor. */
		public ByteStream(IO.WritableByteStream io) {
			this.io = io;
		}
		
		private IO.WritableByteStream io;
		
		@Override
		public IO.Writable getWrappedIO() {
			return (IO.Writable)io;
		}
		
		@Override
		public void close() throws IOException {
			try { io.close(); }
			catch (Exception e) { throw IO.error(e); }
		}
	
		@Override
		public void write(int b) throws IOException {
			io.write((byte)b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			io.write(b, off, len);
		}
	}
	
}
