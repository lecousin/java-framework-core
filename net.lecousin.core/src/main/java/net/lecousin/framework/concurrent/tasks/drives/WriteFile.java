package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.util.Pair;

class WriteFile implements Executable<Integer,IOException> {

	public static Task<Integer, IOException> launch(
		FileAccess file, long pos, ByteBuffer buffer, Priority priority, Consumer<Pair<Integer,IOException>> ondone
	) {
		Task<Integer, IOException> task = new Task<>(file.manager,
			"Write to file " + file.path, priority,
			new WriteFile(file, pos, buffer),
			ondone
		);
		file.openTask.ondone(task, false);
		return task;
	}
	
	public WriteFile(FileAccess file, long pos, ByteBuffer buffer) {
		this.file = file;
		this.pos = pos;
		this.buffer = buffer;
	}
	
	private FileAccess file;
	private long pos;
	private ByteBuffer buffer;
	
	@Override
	public Integer execute(Task<Integer, IOException> taskContext) throws IOException, CancelException {
		try {
			if (pos >= 0)
				file.channel.position(pos);
			int nb = file.channel.write(buffer);
			file.size = file.channel.size();
			if (buffer.remaining() > 0)
				throw new IOException("Only " + nb + " byte(s) written, " + buffer.remaining() + " remaining");
			return Integer.valueOf(nb);
		} catch (ClosedChannelException e) {
			throw new CancelException("File has been closed");
		}
	}
	
}
