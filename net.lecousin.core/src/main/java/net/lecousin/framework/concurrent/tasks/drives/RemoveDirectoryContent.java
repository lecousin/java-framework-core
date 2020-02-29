package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Task to remove the content of a directory (all its files and sub-directories).
 */
public class RemoveDirectoryContent implements Executable<Long,IOException> {

	/** Create task. */
	public static Task<Long, IOException> task(
		File dir, WorkProgress progress, long work, Priority priority, boolean calculateSize
	) {
		return Task.file(dir, "Remove content of directory " + dir.getAbsolutePath(), priority,
			new RemoveDirectoryContent(dir, progress, work, calculateSize), null);
	}
	
	/** Constructor. */
	public RemoveDirectoryContent(File dir, WorkProgress progress, long work, boolean calculateSize) {
		this.dir = dir;
		this.progress = progress;
		this.work = work;
		this.calculateSize = calculateSize;
	}
	
	private File dir;
	private WorkProgress progress;
	private long work;
	private boolean calculateSize;
	
	@Override
	public Long execute(Task<Long, IOException> taskContext) throws IOException, CancelException {
		return Long.valueOf(removeDirectoryContent(dir, progress, work, calculateSize, taskContext));
	}

	static long removeDirectoryContent(File dir, WorkProgress progress, long work, boolean calculateSize, Task<?, ?> taskContext)
	throws IOException, CancelException {
		return remove(dir, progress, work, calculateSize, false, taskContext);
	}
	
	/** Remove a directory with all its content. This must be called in a task OnFile. */
	static long deleteDirectory(File dir, WorkProgress progress, long work, boolean calculateSize, Task<?, ?> taskContext)
	throws IOException, CancelException {
		return remove(dir, progress, work, calculateSize, true, taskContext);
	}
	
	private static long remove(File dir, WorkProgress progress, long work, boolean calculateSize, boolean deleteDir, Task<?, ?> taskContext)
	throws IOException, CancelException {
		try {
			if (!dir.exists())
				return 0;
			File[] files = dir.listFiles();
			if (files == null)
				throw new AccessDeniedException(dir.getAbsolutePath());
			long size = 0;
			int nb = files.length;
			if (deleteDir) nb++;
			for (File f : files) {
				if (taskContext.isCancelling()) throw taskContext.getCancelEvent();
				long step = work / nb--;
				work -= step;
				if (f.isDirectory())
					size += remove(f, progress, step, calculateSize, true, taskContext);
				else {
					if (calculateSize) size += f.length();
					try {
						Files.delete(f.toPath());
					} finally {
						if (progress != null) progress.progress(step);
					}
					if (progress != null && step > 0) progress.progress(step);
				}
			}
			if (deleteDir)
				Files.delete(dir.toPath());
			return size;
		} finally {
			if (progress != null && work > 0)progress.progress(work);
		}
	}
	
}
