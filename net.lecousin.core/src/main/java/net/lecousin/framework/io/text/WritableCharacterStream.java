package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;

/** Implement a non-buffered writable character stream using a writable IO. */
public class WritableCharacterStream implements ICharacterStream.Writable {

	/** Constructor. */
	public WritableCharacterStream(IO.Writable output, Charset charset) {
		this(output, charset.newEncoder());
	}

	/** Constructor. */
	public WritableCharacterStream(IO.Writable output, CharsetEncoder encoder) {
		this.output = output;
		this.encoder = encoder;
	}
	
	private IO.Writable output;
	private CharsetEncoder encoder;
	
	@Override
	public void write(char[] c, int off, int len) throws IOException {
		CharBuffer cb = CharBuffer.wrap(c, off, len);
		ByteBuffer bb = encoder.encode(cb);
		output.writeSync(bb);
	}
	
	@Override
	public void close() throws Exception {
		encoder = null;
		output.close();
		output = null;
	}
	
	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		encoder = null;
		return output.closeAsync();
	}
	
}
