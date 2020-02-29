package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.io.RandomAccessFile;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

class OpenFile implements Executable<Void,IOException> {

	public static Task<Void, IOException> launch(FileAccess file, String mode, Priority priority) {
		return new Task<>(file.manager, "Open file " + file.path, priority, new OpenFile(file, mode), null);
	}
	
	public OpenFile(FileAccess file, String mode) {
		this.mode = mode;
		this.file = file;
	}
	
	private String mode;
	private FileAccess file;
	
	@Override
	public Void execute(Task<Void, IOException> t) throws IOException {
		file.size = file.file.length();
		file.f = new RandomAccessFile(file.file, mode);
		file.channel = file.f.getChannel();
		return null;
	}
	
}
