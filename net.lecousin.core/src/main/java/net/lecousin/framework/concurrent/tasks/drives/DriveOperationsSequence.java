package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;

/**
 * Task to execute a sequence of operation on a file, and avoid separate them into different tasks.
 * The task may be started before all operations are specified if autoStart is set to true.
 * If autoStart is true, the method endOfOperations must be called to indicate that no more 
 * operation will be added.
 * If the method endOfOperations is not yet called and there is no more operation to execute,
 * the task will block until a new operation is added or the method endOfOperations is called.
 */
public class DriveOperationsSequence extends Task<Void,IOException> {

	/** Constructor. */
	public DriveOperationsSequence(TaskManager manager, String description, byte priority, boolean autoStart) {
		super(manager, description, priority);
		this.autoStart = autoStart;
		if (autoStart) sp = new SynchronizationPoint<NoException>();
	}
	
	private boolean autoStart;
	private TurnArray<Operation> operations = new TurnArray<>(10);
	private boolean waiting = false;
	private SynchronizationPoint<NoException> sp;
	private boolean end = false;
	
	/** Append an operation. */
	public void add(Operation operation) {
		synchronized (operations) {
			operations.addLast(operation);
			if (waiting)
				sp.unblock();
		}
		if (autoStart && operations.size() == 1 && getStatus() == Task.STATUS_NOT_STARTED)
			start();
	}
	
	/** Indicate no more operation will be added. */
	public void endOfOperations() {
		end = true;
		if (sp != null) sp.unblock();
	}
	
	@Override
	public Void run() throws IOException {
		do {
			Operation op;
			synchronized (operations) {
				op = operations.pollFirst();
				if (op == null) 
					if (autoStart && !end) waiting = true;
					else break;
			}
			if (op != null) op.execute();
			else {
				if (getApplication().isDebugMode())
					getApplication().getDefaultLogger().debug("DriveOperationsSequence is blocked, consider not using autoStart");
				sp.block(0);
				waiting = false;
				sp.reset();
			}
		} while (!end);
		return null;
	}

	/** Interface for an operation. */
	public static interface Operation {
		/** Return the file on which it works. */
		public FileAccess getFile();
		
		/** Execute the operation. */
		public void execute() throws IOException;
	}
	
	/** Write data to a file. */
	public abstract static class WriteOperation implements Operation {
		/** Return the position where to write. */
		public abstract long getPosition();
		
		/** Return the buffer to write. */
		public abstract ByteBuffer getBuffer();
		
		@Override
		public void execute() throws IOException {
			@SuppressWarnings("resource")
			FileAccess file = getFile();
			long pos = getPosition();
			if (pos < 0)
				file.channel.write(getBuffer());
			else
				file.channel.write(getBuffer(), pos);
			file.size = file.channel.size();
		}
	}
	
	/** Read data from a file. */
	public abstract static class ReadOperation implements Operation {
		/** Return the position from where to read. */
		public abstract long getPosition();
		
		/** Return the buffer to fill. */
		public abstract ByteBuffer getBuffer();
		
		@Override
		public void execute() throws IOException {
			@SuppressWarnings("resource")
			FileAccess file = getFile();
			long pos = getPosition();
			if (pos < 0)
				file.channel.read(getBuffer());
			else
				file.channel.read(getBuffer(), pos);
		}
	}
	
	/** Write a sub-part of a buffer. */
	public static class WriteOperationSubBuffer extends WriteOperation {
		/** Constructor. */
		public WriteOperationSubBuffer(FileAccess file, long position, ByteBuffer buffer, int offset, int len) {
			this.file = file;
			this.position = position;
			this.buffer = buffer;
			this.offset = offset;
			this.len = len;
		}
		
		private FileAccess file;
		private long position;
		private ByteBuffer buffer;
		private int offset;
		private int len;
		
		@Override
		public FileAccess getFile() { return file; }
		
		@Override
		public long getPosition() { return position; }
		
		@Override
		public ByteBuffer getBuffer() {
			buffer.position(offset);
			buffer.limit(offset + len);
			return buffer;
		}
	}
	
	/** Write a long value using {@link ByteBuffer#putLong(long)}. */
	public static class WriteLongOperation extends WriteOperation {
		/** Constructor. */
		public WriteLongOperation(FileAccess file, long position, long value, ByteBuffer usingBuffer) {
			this.file = file;
			this.position = position;
			this.value = value;
			this.buffer = usingBuffer;
		}
		
		private FileAccess file;
		private long position;
		private long value;
		private ByteBuffer buffer;
		
		@Override
		public FileAccess getFile() { return file; }
		
		@Override
		public long getPosition() { return position; }
		
		@Override
		public ByteBuffer getBuffer() {
			buffer.clear();
			buffer.putLong(value);
			buffer.flip();
			return buffer;
		}
	}
	
	/** Write an integer value using {@link ByteBuffer#putInt(int)}. */
	public static class WriteIntegerOperation extends WriteOperation {
		/** Constructor. */
		public WriteIntegerOperation(FileAccess file, long position, int value, ByteBuffer usingBuffer) {
			this.file = file;
			this.position = position;
			this.value = value;
			this.buffer = usingBuffer;
		}
		
		private FileAccess file;
		private long position;
		private int value;
		private ByteBuffer buffer;
		
		@Override
		public FileAccess getFile() { return file; }
		
		@Override
		public long getPosition() { return position; }
		
		@Override
		public ByteBuffer getBuffer() {
			buffer.clear();
			buffer.putInt(value);
			buffer.flip();
			return buffer;
		}
	}
	
}
