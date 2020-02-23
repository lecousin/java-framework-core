package net.lecousin.framework.concurrent.tasks.drives;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
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
	public FileAccess(File file, String mode, Priority priority) {
		this.file = file;
		this.path = file.getAbsolutePath();
		this.priority = priority;
		this.manager = Threading.getDrivesManager().getTaskManager(file);
		if (this.manager == null) this.manager = Threading.getUnmanagedTaskManager();
		openTask = OpenFile.launch(this, mode, priority);
		LCCore.getApplication().toClose(0, this);
	}
	
	RandomAccessFile f;
	FileChannel channel;
	File file;
	String path;
	Priority priority;
	Task<Void, IOException> openTask;
	long size;
	TaskManager manager;
	
	public String getPath() { return path; }
	
	public File getFile() { return file; }
	
	public Task<Void,IOException> getStartingTask() { return openTask; }
	
	public RandomAccessFile getDirectAccess() { return f; }
	
	public Priority getPriority() { return priority; }
	
	public void setPriority(Priority priority) { this.priority = priority; }
	
	@Override
	public String toString() {
		return "FileAccess[" + file.getAbsolutePath() + "]";
	}

	public void open() throws IOException {
		openTask.getOutput().blockException(0);
	}
	
	public IAsync<IOException> closeAsync() {
		LCCore.getApplication().closed(this);
		if (openTask.cancelIfExecutionNotStarted(new CancelException("Close file requested", null)))
			return new Async<>(true);
		Async<IOException> result = new Async<>();
		openTask.getOutput().onDone(() -> {
			if (f != null) {
				try {
					f.close();
				} catch (Exception e) {
					result.error(IO.error(e));
				}
			}
			result.unblock();
		});
		return result;
	}
	
	@Override
	public void close() {
		LCCore.getApplication().closed(this);
		if (openTask.cancelIfExecutionNotStarted(new CancelException("Close file requested", null)))
			return;
		if (!openTask.isDone())
			openTask.getOutput().block(0);
		if (f != null) {
			try { f.close(); }
			catch (Exception e) { /* ignore */ }
			f = null;
		}
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
		SetFileSize.launch(this, newSize, priority).getOutput().blockException(0);
	}
	
	public AsyncSupplier<Void, IOException> setSizeAsync(long newSize) {
		return SetFileSize.launch(this, newSize, priority).getOutput();
	}
	
	public int read(long pos, ByteBuffer buffer) throws IOException {
		try {
			return ReadFile.launch(this, pos, buffer, false, priority, null).getOutput().blockResult(0).intValue();
		} catch (CancelException e) {
			throw IO.error(e);
		}
	}
	
	public AsyncSupplier<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return ReadFile.launch(this, pos, buffer, false, priority, ondone).getOutput();
	}
	
	public int readFully(long pos, ByteBuffer buffer) throws IOException {
		try {
			return ReadFile.launch(this, pos, buffer, true, priority, null).getOutput().blockResult(0).intValue();
		} catch (CancelException e) {
			throw IO.error(e);
		}
	}
	
	public AsyncSupplier<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return ReadFile.launch(this, pos, buffer, true, priority, ondone).getOutput();
	}
	
	public long seek(SeekType type, long move, boolean allowAfterEnd) throws IOException {
		try {
			return SeekFile.launch(this, type, move, allowAfterEnd, true, priority, null).getOutput().blockResult(0).longValue();
		} catch (Exception e) {
			throw IO.error(e);
		}
	}
	
	public AsyncSupplier<Long,IOException> seekAsync(
		SeekType type, long move, boolean allowAfterEnd, Consumer<Pair<Long,IOException>> ondone
	) {
		return SeekFile.launch(this, type, move, allowAfterEnd, true, priority, ondone).getOutput();
	}
	
	public long skip(long move) throws IOException {
		try {
			return SeekFile.launch(this, SeekType.FROM_CURRENT, move, false, false, priority, null)
				.getOutput().blockResult(0).longValue();
		} catch (Exception e) {
			throw IO.error(e);
		}
	}
	
	public AsyncSupplier<Long,IOException> skipAsync(long move, Consumer<Pair<Long,IOException>> ondone) {
		return SeekFile.launch(this, SeekType.FROM_CURRENT, move, false, false, priority, ondone).getOutput();
	}
	
	public int write(long pos, ByteBuffer buffer) throws IOException {
		try {
			return WriteFile.launch(this, pos, buffer, priority, null).getOutput().blockResult(0).intValue();
		} catch (Exception e) {
			throw IO.error(e);
		}
	}
	
	public AsyncSupplier<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return WriteFile.launch(this, pos, buffer, priority, ondone).getOutput();
	}
	
}
