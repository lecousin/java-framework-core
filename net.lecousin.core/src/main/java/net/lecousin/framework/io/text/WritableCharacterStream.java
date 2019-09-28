package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Implement a non-buffered writable character stream using a writable IO. */
public class WritableCharacterStream extends ConcurrentCloseable<IOException> implements ICharacterStream.Writable {

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
	protected IAsync<IOException> closeUnderlyingResources() {
		return output.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		encoder = null;
		output = null;
		ondone.unblock();
	}
	
	@Override
	public byte getPriority() {
		return output.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		output.setPriority(priority);
	}
	
	@Override
	public String getDescription() {
		return output.getSourceDescription();
	}
	
	@Override
	public Charset getEncoding() {
		return encoder.charset();
	}
	
	@Override
	public void writeSync(char[] c, int off, int len) throws IOException {
		CharBuffer cb = CharBuffer.wrap(c, off, len);
		ByteBuffer bb = encoder.encode(cb);
		output.writeSync(bb);
	}
	
	@Override
	public IAsync<IOException> writeAsync(char[] c, int offset, int length) {
		Async<IOException> result = new Async<>();
		operation(new Task.Cpu<Void, NoException>("Encoding characters", getPriority()) {
			@Override
			public Void run() {
				CharBuffer cb = CharBuffer.wrap(c, offset, length);
				ByteBuffer bb;
				try { bb = encoder.encode(cb); }
				catch (CharacterCodingException e) {
					result.error(e);
					return null;
				}
				output.writeAsync(bb).onDone(result);
				return null;
			}
		}).start();
		return result;
	}
	
}
