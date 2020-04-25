package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

class SetFileSize implements Executable<Void,IOException> {

	public static Task<Void, IOException> launch(FileAccess file, long newSize, Priority priority) {
		Task<Void, IOException> task = new Task<>(file.manager, "Change file size", priority, null, new SetFileSize(file, newSize), null);
		file.openTask.ondone(task, false);
		return task;
	}
	
	public SetFileSize(FileAccess file, long newSize) {
		this.file = file;
		this.newSize = newSize;
	}
	
	private FileAccess file;
	private long newSize;
	
	@Override
	public Void execute(Task<Void, IOException> taskContext) throws IOException {
		file.f.setLength(newSize);
		file.size = newSize;
		return null;
	}
	
}
