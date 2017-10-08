package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * A fragmented sub-IO allows to specify a list of fragments inside a seekable IO, and does like those fragments are a contiguous IO.
 * TODO split it with Readable, Writable and ReadWrite
 * TODO improve perf by storing the fragment containing the current position ?
 */
public class FragmentedSubIO extends IO.AbstractIO implements IO.Readable.Seekable, IO.KnownSize {

	/** Constructor. */
	public FragmentedSubIO(IO.Readable.Seekable io, List<RangeLong> fragments, boolean closeParentIOOnClose, String description) {
		this.io = io;
		this.fragments = fragments;
		this.closeParentIOOnClose = closeParentIOOnClose;
		this.description = description;
		size = 0;
		for (RangeLong r : fragments) size += r.max - r.min + 1;
	}
	
	private IO.Readable.Seekable io;
	private List<RangeLong> fragments;
	private long pos = 0;
	private long size;
	private boolean closeParentIOOnClose;
	private String description;
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		if (closeParentIOOnClose) return io.closeAsync();
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return io.canStartReading();
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}
	
	@Override
	public TaskManager getTaskManager() {
		return io.getTaskManager();
	}
	
	@Override
	public byte getPriority() {
		return io.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		io.setPriority(priority);
	}
	
	@Override
	public String getSourceDescription() {
		return description + " in " + io.getSourceDescription();
	}
	
	@Override
	public long getPosition() {
		return pos;
	}
	
	@Override
	public long getSizeSync() {
		return size;
	}
	
	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		AsyncWork<Long, IOException> sp = new AsyncWork<Long, IOException>();
		sp.unblockSuccess(Long.valueOf(getSizeSync()));
		return sp;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Iterator<RangeLong> it = fragments.iterator();
		long p = 0;
		while (it.hasNext()) {
			RangeLong r = it.next();
			long s = r.max - r.min + 1;
			if (pos >= p + s) {
				p += s;
				continue;
			}
			long start = pos - p;
			int len = buffer.remaining();
			if (start + len > s) {
				int prevLimit = buffer.limit();
				buffer.limit((int)(prevLimit - ((start + len) - s)));
				return io.readAsync(r.min + start, buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						buffer.limit(prevLimit);
						if (param.getValue1() != null)
							FragmentedSubIO.this.pos = pos + param.getValue1().intValue();
						if (ondone != null) ondone.run(param);
					}
				});
			}
			return io.readAsync(r.min + start, buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
				@Override
				public void run(Pair<Integer, IOException> param) {
					if (param.getValue1() != null)
						FragmentedSubIO.this.pos = pos + param.getValue1().intValue();
					if (ondone != null) ondone.run(param);
				}
			});
		}
		AsyncWork<Integer,IOException> sp = new AsyncWork<>();
		if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(0), null));
		sp.unblockSuccess(Integer.valueOf(0));
		return sp;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		return readSync(pos, buffer);
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		Iterator<RangeLong> it = fragments.iterator();
		long p = 0;
		while (it.hasNext()) {
			RangeLong r = it.next();
			long s = r.max - r.min + 1;
			if (pos >= p + s) {
				p += s;
				continue;
			}
			long start = pos - p;
			int len = buffer.remaining();
			if (start + len > s) {
				int prevLimit = buffer.limit();
				buffer.limit((int)(prevLimit - ((start + len) - s)));
				len = io.readSync(r.min + start, buffer);
				buffer.limit(prevLimit);
			} else {
				len = io.readSync(r.min + start, buffer);
			}
			this.pos = pos + len;
			return len;
		}
		return 0;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		Iterator<RangeLong> it = fragments.iterator();
		long p = 0;
		int done = 0;
		while (it.hasNext()) {
			RangeLong r = it.next();
			long s = r.max - r.min + 1;
			if (pos >= p + s) {
				p += s;
				continue;
			}
			long start = pos - p;
			int len = buffer.remaining();
			if (start + len > s) {
				int prevLimit = buffer.limit();
				buffer.limit((int)(prevLimit - ((start + len) - s)));
				len = io.readFullySync(r.min + start, buffer);
				buffer.limit(prevLimit);
			} else {
				len = io.readFullySync(r.min + start, buffer);
			}
			this.pos = pos + len;
			done += len;
			if (!buffer.hasRemaining()) return done;
			// continue on next fragment
			pos += len;
			p += s;
		}
		return done;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readFullyAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsynch(this, pos, buffer, ondone);
	}
	
	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			pos = move;
			break;
		case FROM_END:
			pos = size - move;
			break;
		case FROM_CURRENT:
			pos += move;
			break;
		default: break;
		}
		if (pos > size) pos = size;
		if (pos < 0) pos = 0;
		return pos;
	}
	
	@Override
	public long skipSync(long n) {
		long size = getSizeSync();
		// skip checkstyle: VariableDeclarationUsageDistance
		long prevPos = pos;
		pos += n;
		if (pos > size) pos = size;
		if (pos < 0) pos = 0;
		return pos - prevPos;
	}
	
	@Override
	public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		AsyncWork<Long,IOException> sp = new AsyncWork<>();
		seekSync(type, move);
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(pos), null));
		sp.unblockSuccess(Long.valueOf(pos));
		return sp;
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
		AsyncWork<Long,IOException> sp = new AsyncWork<>();
		long skipped = skipSync(n);
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skipped), null));
		sp.unblockSuccess(Long.valueOf(skipped));
		return sp;
	}
	
}
