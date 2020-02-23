package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.progress.FakeWorkProgress;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Task to remove a directory and all its content.
 */
public class RemoveDirectory implements Executable<Long,IOException> {

	/** Create task. */
	public static Task<Long, IOException> task(
		File dir, WorkProgress progress, long work, String progressSubText, Priority priority, boolean calculateSize
	) {
		return Task.file(dir, "Remove directory " + dir.getAbsolutePath(), priority,
			new RemoveDirectory(dir, progress, work, progressSubText, calculateSize));
	}
	
	/** Constructor. */
	public RemoveDirectory(File dir, WorkProgress progress, long work, String progressSubText, boolean calculateSize) {
		this.dir = dir;
		if (progress == null) progress = new FakeWorkProgress();
		this.progress = progress;
		this.work = work;
		if (progressSubText == null) progressSubText = "";
		this.progressSubText = progressSubText;
		this.calculateSize = calculateSize;
	}
	
	private File dir;
	private WorkProgress progress;
	private long work;
	private String progressSubText;
	private boolean calculateSize;
	
	@Override
	public Long execute() throws IOException {
		String prev = progress != null ? progress.getSubText() : null;
		if (progress != null && progressSubText != null) progress.setSubText(progressSubText);
		long removedSize = RemoveDirectoryContent.deleteDirectory(dir, progress, work, calculateSize);
		if (progress != null) progress.setSubText(prev);
		return Long.valueOf(removedSize);
	}
	
}
