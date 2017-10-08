package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;

import net.lecousin.framework.concurrent.Task;

class SetFileSizeTask extends Task.OnFile<Void,IOException> {

	public SetFileSizeTask(FileAccess file, long newSize, byte priority) {
		super(file.manager, "Change file size", priority);
		this.file = file;
		this.newSize = newSize;
		file.openTask.ondone(this, false);
	}
	
	private FileAccess file;
	private long newSize;
	
	@Override
	public Void run() throws IOException {
		file.f.setLength(newSize);
		file.size = newSize;
		return null;
	}
	
}
