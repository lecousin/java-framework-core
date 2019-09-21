package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;

/** Task to remove a file. */
public class RemoveFileTask extends Task.OnFile<Void,IOException> {
	
	/** Constructor. */
	public RemoveFileTask(File file, byte priority) {
		super(file, "Removing file", priority);
		this.file = file;
	}
	
	private File file;
	
	@Override
	public Void run() throws IOException {
		if (!file.delete() && file.exists())
			throw new IOException("Unable to remove file");
		return null;
	}

}
