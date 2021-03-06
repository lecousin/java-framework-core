package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.Pair;

class SeekFile implements Executable<Long,IOException> {

	/**
	 * Create task.
	 * @param file file
	 * @param type type of seek
	 * @param move amount
	 * @param allowAfterEnd true to allow seeking beyond the end of the file
	 * @param returnNewPosition if false, the difference between previous position and new position is returned
	 * @param priority task priority
	 * @param ondone listener to be called before to return the result, or null
	 */
	public static Task<Long, IOException> launch(
		FileAccess file, SeekType type, long move, boolean allowAfterEnd, boolean returnNewPosition,
		Priority priority, Consumer<Pair<Long,IOException>> ondone
	) {
		Task<Long, IOException> task = new Task<>(file.manager, "Seek in file", priority,
			new SeekFile(file, type, move, allowAfterEnd, returnNewPosition), ondone);
		file.openTask.ondone(task, false);
		return task;
	}
	
	public SeekFile(
		FileAccess file, SeekType type, long move, boolean allowAfterEnd, boolean returnNewPosition
	) {
		this.file = file;
		this.type = type;
		this.move = move;
		this.allowAfterEnd = allowAfterEnd;
		this.returnNewPosition = returnNewPosition;
	}
	
	private FileAccess file;
	private SeekType type;
	private long move;
	private boolean allowAfterEnd;
	private boolean returnNewPosition;
	
	@Override
	public Long execute(Task<Long, IOException> taskContext) throws IOException, CancelException {
		try {
			long initialPos = -1;
			if (!returnNewPosition) initialPos = file.channel.position();
			long size = -1;
			if (!allowAfterEnd) size = file.getSize();
			switch (type) {
			case FROM_BEGINNING:
				if (move < 0) move = 0;
				if (!allowAfterEnd && move > size) move = size;
				file.channel.position(move);
				break;
			case FROM_CURRENT:
				long pos = file.channel.position();
				if (pos + move < 0) move = -pos;
				if (!allowAfterEnd && pos + move > size) move = size - pos;
				file.channel.position(pos + move);
				break;
			default: //case FROM_END:
				if (move < 0) move = 0;
				if (size == -1) size = file.getSize();
				if (size - move < 0) move = size;
				file.channel.position(size - move);
				break;
			}
			if (returnNewPosition)
				return Long.valueOf(file.channel.position());
			return Long.valueOf(file.channel.position() - initialPos);
		} catch (ClosedChannelException e) {
			throw new CancelException("File has been closed");
		}
	}
	
}
