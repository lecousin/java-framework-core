package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

/** Task to read all bytes from a file. */
public class FullReadFileTask implements Executable<byte[],IOException> {

	/** Create task. */
	public static Task<byte[],IOException> create(File file, Priority priority) {
		return Task.file(file, "Read full content of " + file.getAbsolutePath(), priority, new FullReadFileTask(file), null);
	}
	
	/** Constructor. */
	public FullReadFileTask(File file) {
		this.file = file;
	}
	
	private File file;
	
	@Override
	public byte[] execute() throws IOException {
		try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
			byte[] content = new byte[(int)file.length()];
			int pos = 0;
			do {
				int nb = f.read(content, pos, content.length - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < content.length);
			return content;
		}
	}
	
}
