package net.lecousin.framework.io.util;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.PositionKnownWrapper;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Wraps an IO.Readable and make a WorkProgress progress while data is read on it.
 */
public class ReadableWithProgress extends PositionKnownWrapper.Readable implements IO.Readable {
	
	/** Constructor. */
	public ReadableWithProgress(IO.Readable io, long size, WorkProgress progress, long work) {
		super(io, 0);
		this.size = size;
		this.progress = progress;
		this.work = work;
		prevWork = 0;
		if (progress != null)
			addPositionChangedListener(positionChanged);
	}

	protected long size;
	protected WorkProgress progress;
	protected long work;
	
	protected long prevWork;
	
	protected Runnable positionChanged = () -> {
		long pos = position.get();
		long w = pos * work / size;
		if (w != prevWork) {
			progress.progress(w - prevWork);
			prevWork = w;
		}
	};

}
