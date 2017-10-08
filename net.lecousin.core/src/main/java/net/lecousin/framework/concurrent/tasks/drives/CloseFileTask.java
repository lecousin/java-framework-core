package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;

class CloseFileTask extends Task./*OnFile*/Cpu<Void,IOException> {

	public CloseFileTask(FileAccess file) {
		/*super(file.manager, "Close file", Task.PRIORITY_NORMAL);*/
		super("Close file", Task.PRIORITY_NORMAL);
		if (file.openTask.getStatus() < Task.STATUS_RUNNING) 
			if (file.openTask.cancelIfExecutionNotStarted(new CancelException("Close file requested", null))) {
				setDone(null, null);
				return;
			}
		this.file = file;
		file.openTask.ondone(this, true);
	}
	
	private FileAccess file;
	
	@Override
	public Void run() throws IOException {
		if (file.f != null) // case it was not yet open
			file.f.close();
		return null;
	}
	
}
