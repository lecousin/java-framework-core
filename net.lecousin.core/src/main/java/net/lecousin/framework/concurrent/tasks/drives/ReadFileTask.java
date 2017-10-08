package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import net.lecousin.framework.collections.sort.RedBlackTreeInteger;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Task to read some bytes from a file.
 */
class ReadFileTask extends Task.OnFile<Integer,IOException> {

	/** Constructor. */
	public ReadFileTask(
		FileAccess file, long pos, ByteBuffer buffer, boolean fully,
		byte priority, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		super(file.manager, "Read from file " + file.path + " at " + pos, priority, ondone);
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
	private int nbRead = 0;
	
	private RedBlackTreeInteger<SynchronizationPoint<NoException>> waiting = null;
	private ArrayList<Listener<Integer>> onprogress = null;
	
	public int getCurrentNbRead() { return nbRead; }
	
	public int waitNbRead(int nbRead) throws IOException {
		if (this.nbRead >= nbRead) return this.nbRead;
		if (getError() != null) throw getError();
		if (isDone()) return this.nbRead;
		SynchronizationPoint<NoException> sp = new SynchronizationPoint<NoException>();
		synchronized (buffer) {
			if (getError() != null) throw getError();
			if (isDone()) return this.nbRead;
			if (waiting == null) waiting = new RedBlackTreeInteger<>();
			waiting.add(nbRead, sp);
		}
		sp.block(0);
		if (getError() != null) throw getError();
		return this.nbRead;
	}
	
	public void onprogress(Listener<Integer> listener) {
		synchronized (buffer) {
			if (nbRead > 0) listener.fire(Integer.valueOf(nbRead));
			if (onprogress == null) onprogress = new ArrayList<>(5);
			onprogress.add(listener);
		}
	}
	
	@Override
	public Integer run() throws IOException {
		try {
			if (!file.openTask.isSuccessful())
				throw file.openTask.getError();
			nbRead = 0;
			if (pos >= 0)
				try { file.channel.position(pos); }
				catch (IOException e) {
					throw new IOException("Unable to seek to position " + pos + " in file " + file.path, e);
				}
			if (!fully) {
				nbRead = file.channel.read(buffer);
				callListeners();
			} else {
				nbRead = 0;
				while (buffer.remaining() > 0) {
					int nb = file.channel.read(buffer);
					if (nb <= 0) break;
					nbRead += nb;
					callListeners();
				}
			}
			return Integer.valueOf(nbRead);
		} finally {
			synchronized (buffer) {
				if (waiting != null) {
					for (SynchronizationPoint<NoException> sp : waiting) sp.unblock();
					waiting = null;
				}
			}
		}
	}
	
	private void callListeners() {
		synchronized (buffer) {
			if (onprogress != null)
				for (int i = onprogress.size() - 1; i >= 0; --i)
					onprogress.get(i).fire(Integer.valueOf(nbRead));
			if (waiting != null) {
				do {
					RedBlackTreeInteger.Node<SynchronizationPoint<NoException>> min = waiting.getMin();
					if (min.getValue() <= nbRead) {
						min.getElement().unblock();
						waiting.removeMin();
					} else
						break;
				} while (!waiting.isEmpty());
				if (waiting.isEmpty()) waiting = null;
			}
		}
	}
	
}
