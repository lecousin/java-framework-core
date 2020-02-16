package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Task to remove the content of a directory (all its files and sub-directories).
 */
public class RemoveDirectoryContentTask extends Task.OnFile<Long,IOException> {

	/** Constructor. */
	public RemoveDirectoryContentTask(File dir, WorkProgress progress, long work, byte priority, boolean calculateSize) {
		super(dir, "Removing directory", priority);
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
	public Long run() throws IOException {
		return Long.valueOf(removeDirectoryContent(dir, progress, work, calculateSize));
	}

	static long removeDirectoryContent(File dir, WorkProgress progress, long work, boolean calculateSize) throws IOException {
		try {
			if (!dir.exists())
				return 0;
			File[] files = dir.listFiles();
			if (files == null)
				throw new AccessDeniedException(dir.getAbsolutePath());
			int nb = files.length;
			long size = 0;
			for (File f : files) {
				long step = work / nb--;
				work -= step;
				if (f.isDirectory()) {
					size += deleteDirectory(f, progress, step, calculateSize);
				} else {
					if (calculateSize) size += f.length();
					try {
						Files.delete(f.toPath());
					} finally {
						if (progress != null) progress.progress(step);
					}
				}
			}
			return size;
		} finally {
			if (progress != null && work > 0) progress.progress(work);
		}
	}
	
	/** Remove a directory with all its content. This must be called in a task OnFile. */
	public static long deleteDirectory(File dir, WorkProgress progress, long work, boolean calculateSize) throws IOException {
		try {
			if (!dir.exists())
				return 0;
			File[] files = dir.listFiles();
			if (files == null)
				throw new AccessDeniedException(dir.getAbsolutePath());
			long size = 0;
			int nb = 1 + files.length;
			for (File f : files) {
				long step = work / nb--;
				work -= step;
				if (f.isDirectory())
					size += deleteDirectory(f, progress, step, calculateSize);
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
			Files.delete(dir.toPath());
			return size;
		} finally {
			if (progress != null && work > 0)progress.progress(work);
		}
	}
	
}
