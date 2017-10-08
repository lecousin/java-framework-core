package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.io.RandomAccessFile;

import net.lecousin.framework.concurrent.Task;

class OpenFileTask extends Task.OnFile<Void,IOException> {

	public OpenFileTask(FileAccess file, String mode, byte priority) {
		super(file.manager, "Open file " + file.path, priority);
		this.mode = mode;
		this.file = file;
	}
	
	private String mode;
	private FileAccess file;
	
	@Override
	public Void run() throws IOException {
		file.size = file.file.length();
		file.f = new RandomAccessFile(file.file, mode);
		file.channel = file.f.getChannel();
		return null;
	}
	
}
