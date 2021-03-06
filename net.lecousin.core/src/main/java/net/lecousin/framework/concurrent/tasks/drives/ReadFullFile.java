package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.memory.ByteArrayCache;

/** Task to read all bytes from a file. */
public class ReadFullFile implements Executable<byte[],IOException> {

	/** Create task. */
	public static Task<byte[],IOException> create(File file, Priority priority) {
		return Task.file(file, "Read full content of " + file.getAbsolutePath(), priority, new ReadFullFile(file), null);
	}
	
	/** Constructor. */
	public ReadFullFile(File file) {
		this.file = file;
	}
	
	private File file;
	
	@Override
	public byte[] execute(Task<byte[], IOException> taskContext) throws IOException, CancelException {
		try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
			byte[] content = ByteArrayCache.getInstance().get((int)file.length(), false);
			int pos = 0;
			do {
				if (taskContext.isCancelling()) throw taskContext.getCancelEvent();
				int nb = f.read(content, pos, content.length - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < content.length);
			return content;
		}
	}
	
}
