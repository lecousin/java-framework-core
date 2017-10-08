package net.lecousin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Implements Writable from an OutputStream.
 */
public class IOFromOutputStream extends IO.AbstractIO implements IO.Writable {

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
	public ISynchronizationPoint<IOException> canStartWriting() {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		int nb = buffer.remaining();
		if (buffer.hasArray()) {
			stream.write(buffer.array(), buffer.position(), buffer.remaining());
			buffer.position(buffer.position() + nb);
		} else {
			byte[] buf = new byte[buffer.remaining()];
			buffer.get(buf);
			stream.write(buf);
		}
		return nb;
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		return IOUtil.writeAsyncUsingSync(this, buffer, ondone).getSynch();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		return IOUtil.closeAsync(stream);
	}
	
}
