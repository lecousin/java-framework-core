package net.lecousin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Implements Writable from an OutputStream.
 */
public class IOFromOutputStream extends ConcurrentCloseable<IOException> implements IO.Writable {

	/** Constructor. */
	public IOFromOutputStream(OutputStream stream, String sourceDescription, TaskManager manager, byte priority) {
		this.stream = stream;
		this.sourceDescription = sourceDescription;
		this.manager = manager;
		this.priority = priority;
	}
	
	private OutputStream stream;
	private String sourceDescription;
	private TaskManager manager;
	private byte priority;
	
	public OutputStream getOutputStream() {
		return stream;
	}
	
	@Override
	public String getSourceDescription() {
		return sourceDescription;
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public void setPriority(byte priority) {
		this.priority = priority;
	}
	
	@Override
	public TaskManager getTaskManager() {
		return manager;
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
		return new Async<>(true);
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		int nb = buffer.remaining();
		if (buffer.hasArray()) {
			stream.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
			buffer.position(buffer.position() + nb);
		} else {
			byte[] buf = new byte[buffer.remaining()];
			buffer.get(buf);
			stream.write(buf);
		}
		return nb;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return operation(IOUtil.writeAsyncUsingSync(this, buffer, ondone)).getOutput();
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return IOUtil.closeAsync(stream);
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		stream = null;
		ondone.unblock();
	}
	
}
