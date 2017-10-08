package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.lecousin.framework.concurrent.Task;

/** Task to read all bytes from a file. */
public class FullReadFileTask extends Task.OnFile<byte[],IOException> {

	/** Constructor. */
	public FullReadFileTask(File file, byte priority) {
		super(file, "Read full content of " + file.getAbsolutePath(), priority);
		this.file = file;
	}
	
	private File file;
	
	@Override
	public byte[] run() throws IOException {
		try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
			byte[] content = new byte[(int)file.length()];
			int pos = 0;
			do {
				int nb = f.read(content, pos, content.length - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < content.length);
			f.close();
			return content;
		}
	}
	
}
