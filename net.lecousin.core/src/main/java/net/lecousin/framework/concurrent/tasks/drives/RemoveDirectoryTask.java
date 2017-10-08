package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.progress.FakeWorkProgress;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Task to remove a directory and all its content.
 */
public class RemoveDirectoryTask extends Task.OnFile<Long,IOException> {

	/** Constructor. */
	public RemoveDirectoryTask(File dir, WorkProgress progress, long work, String progressSubText, byte priority, boolean calculateSize) {
		super(dir, "Remove directory", priority);
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
	public Long run() throws IOException {
		String prev = progress != null ? progress.getSubText() : null;
		if (progressSubText != null) progress.setSubText(progressSubText);
		long removedSize = RemoveDirectoryContentTask.deleteDirectory(dir, progress, work, calculateSize);
		if (progress != null) progress.setSubText(prev);
		return Long.valueOf(removedSize);
	}
	
}
