package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;

/**
 * Task to read some bytes from a file.
 */
class ReadFileTask extends Task.OnFile<Integer,IOException> {

	/** Constructor. */
	public ReadFileTask(
		FileAccess file, long pos, ByteBuffer buffer, boolean fully,
		byte priority, Consumer<Pair<Integer,IOException>> ondone
	) {
		super(file.manager, "Read from file " + file.path + (pos >= 0 ? " at " + pos : ""), priority, ondone);
		this.file = file;
		this.pos = pos;
		this.buffer = buffer;
		this.fully = fully;
		file.openTask.ondone(this, false);
	}
	
	private FileAccess file;
	private long pos;
	private ByteBuffer buffer;
	private boolean fully;
	
	@Override
	public Integer run() throws IOException, CancelException {
		if (!file.openTask.isSuccessful())
			throw file.openTask.getError();
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
