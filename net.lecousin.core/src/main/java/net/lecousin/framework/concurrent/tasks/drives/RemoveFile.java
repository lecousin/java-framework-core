package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

/** Task to remove a file. */
public class RemoveFile implements Executable<Void,IOException> {
	
	/** Create task. */
	public static Task<Void, IOException> task(File file, Priority priority) {
		return Task.file(file, "Remove file " + file.getAbsolutePath(), priority, new RemoveFile(file));
	}
	
	/** Constructor. */
	public RemoveFile(File file) {
		this.file = file;
	}
	
	private File file;
	
	@Override
	public Void execute() throws IOException {
		Files.deleteIfExists(file.toPath());
		return null;
	}

}
