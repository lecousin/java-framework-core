package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

class WriteFileTask extends Task.OnFile<Integer,IOException> {

	public WriteFileTask(FileAccess file, long pos, ByteBuffer buffer, byte priority, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		super(file.manager, "Write to file " + file.path, priority, ondone);
		this.file = file;
		this.pos = pos;
		this.buffer = buffer;
		file.openTask.ondone(this, false);
	}
	
	private FileAccess file;
	private long pos;
	private ByteBuffer buffer;
	
	@Override
	public Integer run() throws IOException, CancelException {
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
