package net.lecousin.framework.concurrent.tasks.drives;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.Pair;

// skip checkstyle: JavadocMethod
/**
 * This class is used as a bridge between {@link FileIO} and dedicated tasks.
 * This should not be used directly, but {@link FileIO} should be used instead.
 */
public class FileAccess implements AutoCloseable, Closeable {

	/** Constructor. */
	public FileAccess(File file, String mode, byte priority) {
		this.file = file;
		this.path = file.getAbsolutePath();
		this.priority = priority;
		this.manager = Threading.getDrivesTaskManager().getTaskManager(file);
		if (this.manager == null) this.manager = Threading.getUnmanagedTaskManager();
		openTask = new OpenFileTask(this, mode, priority);
		LCCore.getApplication().toClose(0, this);
	}
	
	RandomAccessFile f;
	FileChannel channel;
	File file;
	String path;
	byte priority;
	OpenFileTask openTask;
	long size;
	TaskManager manager;
	
	public String getPath() { return path; }
	
	public File getFile() { return file; }
	
	public Task<Void,IOException> getStartingTask() { return openTask; }
	
	public RandomAccessFile getDirectAccess() { return f; }
	
	public byte getPriority() { return priority; }
	
	public void setPriority(byte priority) { this.priority = priority; }
	
	@Override
	public String toString() {
		return "FileAccess[" + file.getAbsolutePath() + "]";
	}

	public void open() throws IOException {
		openTask.getOutput().blockException(0);
	}
	
	public Task<Void,IOException> closeAsync() {
		return new CloseFileTask(this);
	}
	
	@Override
	public void close() {
		closeAsync().getOutput().block(0);
	}
	
	public long getPosition() throws IOException {
		if (!openTask.isDone()) return 0;
		if (!openTask.isSuccessful()) throw openTask.getError();
		return channel.position();
	}
	
	public long getSize() throws IOException {
		openTask.getOutput().blockException(0);
		return size;
	}
	
	public void getSize(final AsyncSupplier<Long,IOException> sp) {
		Runnable ready = () -> {
			if (openTask.isSuccessful())
				sp.unblockSuccess(Long.valueOf(size));
			else
				sp.unblockError(openTask.getError());
		};
		openTask.getOutput().onDone(ready);
	}
	
	public void setSize(long newSize) throws IOException {
		SetFileSizeTask t = new SetFileSizeTask(this, newSize, priority);
		t.getOutput().blockException(0);
	}
	
	public Task<Void,IOException> setSizeAsync(long newSize) {
		return new SetFileSizeTask(this, newSize, priority);
	}
	
	public int read(long pos, ByteBuffer buffer) throws IOException {
		try {
			return new ReadFileTask(this, pos, buffer, false, priority, null).getOutput().blockResult(0).intValue();
		} catch (CancelException e) {
			throw IO.error(e);
		}
	}
	
	public Task<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return new ReadFileTask(this, pos, buffer, false, priority, ondone);
	}
	
	public int readFully(long pos, ByteBuffer buffer) throws IOException {
		try {
			return new ReadFileTask(this, pos, buffer, true, priority, null).getOutput().blockResult(0).intValue();
		} catch (CancelException e) {
			throw IO.error(e);
		}
	}
	
	public Task<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return new ReadFileTask(this, pos, buffer, true, priority, ondone);
	}
	
	public long seek(SeekType type, long move, boolean allowAfterEnd) throws IOException {
		SeekFileTask task = new SeekFileTask(this, type, move, allowAfterEnd, true, priority, null);
		task.getOutput().blockException(0);
		return task.getResult().longValue();
	}
	
	public Task<Long,IOException> seekAsync(
		SeekType type, long move, boolean allowAfterEnd, Consumer<Pair<Long,IOException>> ondone
	) {
		return new SeekFileTask(this, type, move, allowAfterEnd, true, priority, ondone);
	}
	
	public long skip(long move) throws IOException {
		SeekFileTask task = new SeekFileTask(this, SeekType.FROM_CURRENT, move, false, false, priority, null);
		task.getOutput().blockException(0);
		return task.getResult().longValue();
	}
	
	public Task<Long,IOException> skipAsync(long move, Consumer<Pair<Long,IOException>> ondone) {
		return new SeekFileTask(this, SeekType.FROM_CURRENT, move, false, false, priority, ondone);
	}
	
	public int write(long pos, ByteBuffer buffer) throws IOException {
		WriteFileTask task = new WriteFileTask(this, pos, buffer, priority, null);
		task.getOutput().blockException(0);
		return task.getResult().intValue();
	}
	
	public Task<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return new WriteFileTask(this, pos, buffer, priority, ondone);
	}
	
}
