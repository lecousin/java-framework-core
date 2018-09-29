package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IOUtil;

/**
 * Task to create a directory.
 */
public class RenameFileTask extends Task.OnFile<Void,IOException> {
	
	/** Rename a file.
	 * It may do a copy then a delete, or a simple rename, depending if the source and destination are
	 * on the same drive or not.
	 */
	public static ISynchronizationPoint<IOException> rename(File source, File destination, byte priority) {
		// TODO we should use the roots instead of drive
		TaskManager t1 = Threading.getDrivesTaskManager().getTaskManager(source);
		TaskManager t2 = Threading.getDrivesTaskManager().getTaskManager(destination);
		if (t1 == t2)
			return new RenameFileTask(t1, source, destination, priority).start().getOutput();
		AsyncWork<Long, IOException> copy = IOUtil.copy(source, destination, priority, source.length(), null, 0, null);
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		copy.listenInline(() -> {
			new RemoveFileTask(source, priority).start().getOutput().listenInline(result);
		}, result);
		return result;
	}

	/** Constructor. */
	private RenameFileTask(TaskManager tm, File source, File destination, byte priority) {
		super(tm, "Rename file", priority);
		this.source = source;
		this.destination = destination;
	}
	
	private File source;
	private File destination;
	
	@Override
	public Void run() throws IOException {
		if (!source.renameTo(destination))
			throw new IOException("Unable to rename file " + source.getAbsolutePath() + " into " + destination.getAbsolutePath());
		return null;
	}
	
}
