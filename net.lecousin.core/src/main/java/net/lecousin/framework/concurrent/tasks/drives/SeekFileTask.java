package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

class SeekFileTask extends Task.OnFile<Long,IOException> {

	/**
	 *  Constructor.
	 * @param file file
	 * @param type type of seek
	 * @param move amount
	 * @param allowAfterEnd true to allow seeking beyond the end of the file
	 * @param returnNewPosition if false, the difference between previous position and new position is returned
	 * @param priority task priority
	 * @param ondone listener to be called before to return the result, or null
	 */
	public SeekFileTask(
		FileAccess file, SeekType type, long move, boolean allowAfterEnd, boolean returnNewPosition,
		byte priority, RunnableWithParameter<Pair<Long,IOException>> ondone
	) {
		super(file.manager, "Seek in file", priority, ondone);
		this.file = file;
		this.type = type;
		this.move = move;
		this.allowAfterEnd = allowAfterEnd;
		this.returnNewPosition = returnNewPosition;
		file.openTask.ondone(this, false);
	}
	
	private FileAccess file;
	private SeekType type;
	private long move;
	private boolean allowAfterEnd;
	private boolean returnNewPosition;
	
	@Override
	public Long run() throws IOException {
		long initialPos = -1;
		if (!returnNewPosition) initialPos = file.channel.position();
		long size = -1;
		if (!allowAfterEnd) size = file.getSize();
		switch (type) {
		case FROM_BEGINNING:
			if (move < 0) move = 0;
			if (!allowAfterEnd)
				if (move > size) move = size;
			file.channel.position(move);
			break;
		case FROM_CURRENT:
			long pos = file.channel.position();
			if (pos + move < 0) move = -pos;
			if (!allowAfterEnd)
				if (pos + move > size) move = size - pos;
			file.channel.position(pos + move);
			break;
		case FROM_END:
			if (move < 0) move = 0;
			if (size == -1) size = file.getSize();
			if (size - move < 0) move = size;
			file.channel.position(size - move);
			break;
		default: break;
		}
		if (returnNewPosition)
			return Long.valueOf(file.channel.position());
		return Long.valueOf(file.channel.position() - initialPos);
	}
	
}
