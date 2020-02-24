package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;

/**
 * Task to read some bytes from a file.
 */
class ReadFile implements Executable<Integer,IOException> {

	public static Task<Integer, IOException> launch(
		FileAccess file, long pos, ByteBuffer buffer, boolean fully,
		Priority priority, Consumer<Pair<Integer,IOException>> ondone
	) {
		Task<Integer, IOException> task = new Task<>(file.manager,
			"Read from file " + file.path + (pos >= 0 ? " at " + pos : ""), priority,
			new ReadFile(file, pos, buffer, fully),
			ondone);
		file.openTask.ondone(task, false);
		return task;
	}
	
	/** Constructor. */
	public ReadFile(
		FileAccess file, long pos, ByteBuffer buffer, boolean fully
	) {
		this.file = file;
		this.pos = pos;
		this.buffer = buffer;
		this.fully = fully;
	}
	
	private FileAccess file;
	private long pos;
	private ByteBuffer buffer;
	private boolean fully;
	
	@Override
	public Integer execute() throws IOException, CancelException {
		if (!file.openTask.isSuccessful())
			throw file.openTask.getOutput().getError();
		int nbRead = 0;
		if (pos >= 0)
			try { file.channel.position(pos); }
			catch (ClosedChannelException e) { throw IO.cancelClosed(); }
			catch (IOException e) {
				throw new IOException("Unable to seek to position " + pos + " in file " + file.path, e);
			}
		if (!fully) {
			try { nbRead = file.channel.read(buffer); }
			catch (ClosedChannelException e) { throw IO.cancelClosed(); }
		} else {
			nbRead = 0;
			while (buffer.remaining() > 0) {
				int nb;
				try { nb = file.channel.read(buffer); }
				catch (ClosedChannelException e) { throw IO.cancelClosed(); }
				if (nb <= 0) break;
				nbRead += nb;
			}
		}
		return Integer.valueOf(nbRead);
	}
	
}
