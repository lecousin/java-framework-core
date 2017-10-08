package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

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
		if (!dir.exists()) {
			if (progress != null) progress.progress(work);
			return 0;
		}
		File[] files = dir.listFiles();
		if (files == null) {
			if (progress != null) progress.progress(work);
			throw new AccessDeniedException(dir.getAbsolutePath());
		}
		int nb = files.length;
		long size = 0;
		for (File f : files) {
			long step = work / nb--;
			work -= step;
			if (f.isDirectory()) {
				try { size += deleteDirectory(f, progress, step, calculateSize); }
				catch (IOException e) {
					if (progress != null) progress.progress(work);
					throw e;
				}
			} else {
				if (calculateSize) size += f.length();
				if (!f.delete()) {
					if (progress != null) progress.progress(work + step);
					throw new IOException("Unable to delete file " + f.getAbsolutePath());
				}
				if (progress != null) progress.progress(step);
			}
		}
		if (progress != null && work > 0) progress.progress(work);
		return size;
	}
	
	/** Remove a directory with all its content. This must be called in a task OnFile. */
	public static long deleteDirectory(File dir, WorkProgress progress, long work, boolean calculateSize) throws IOException {
		if (!dir.exists()) {
			if (progress != null) progress.progress(work);
			return 0;
		}
		File[] files = dir.listFiles();
		if (files == null) {
			if (progress != null) progress.progress(work);
			throw new AccessDeniedException(dir.getAbsolutePath());
		}
		long size = 0;
		int nb = 1 + files.length;
		for (File f : files) {
			long step = work / nb--;
			work -= step;
			if (f.isDirectory())
				try { size += deleteDirectory(f, progress, step, calculateSize); }
				catch (IOException e) {
					if (progress != null)progress.progress(work);
					throw e;
				}
			else {
				if (calculateSize) size += f.length();
				if (!f.delete() && f.exists()) {
					if (progress != null) progress.progress(work + step);
					throw new IOException("Unable to delete file " + f.getAbsolutePath());
				}
				if (progress != null && step > 0) progress.progress(step);
			}
		}
		if (!dir.delete() && dir.exists()) {
			if (progress != null && work > 0) progress.progress(work);
			throw new IOException("Unable to delete directory " + dir.getAbsolutePath());
		}
		if (progress != null && work > 0)progress.progress(work);
		return size;
	}
	
}
