package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

/**
 * Task to create a directory.
 */
public class CreateDirectory implements Executable<Void,IOException> {

	/** Create task. */
	public static Task<Void, IOException> task(File dir, boolean recursive, boolean failIfExists, Priority priority) {
		return Task.file(dir, "Create directory", priority, new CreateDirectory(dir, recursive, failIfExists));
	}
	
	/** Constructor. */
	public CreateDirectory(File dir, boolean recursive, boolean failIfExists) {
		this.dir = dir;
		this.recursive = recursive;
		this.failIfExists = failIfExists;
	}
	
	private File dir;
	private boolean recursive;
	private boolean failIfExists;
	
	@Override
	public Void execute() throws IOException {
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
