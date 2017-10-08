package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;

/**
 * Task to create a directory.
 */
public class CreateDirectoryTask extends Task.OnFile<Void,IOException> {

	/** Constructor. */
	public CreateDirectoryTask(File dir, boolean recursive, boolean failIfExists, byte priority) {
		super(dir, "Create directory", priority);
		this.dir = dir;
		this.recursive = recursive;
		this.failIfExists = failIfExists;
	}
	
	private File dir;
	private boolean recursive;
	private boolean failIfExists;
	
	@Override
	public Void run() throws IOException {
		boolean created;
		if (recursive)
			created = dir.mkdirs();
		else
			created = dir.mkdir();
		if (!dir.exists())
			throw new IOException("Directory not created");
		else if (!created && failIfExists)
			throw new IOException("Directory already exists");
		return null;
	}
	
}
