package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * A fragmented sub-IO allows to specify a list of fragments inside a seekable IO, and does like those fragments are a contiguous IO.
 * TODO improve perf by storing the fragment containing the current position ?
 */
public abstract class FragmentedSubIO extends ConcurrentCloseable<IOException> implements IO.KnownSize, IO.Seekable {

	/** Constructor. */
	public FragmentedSubIO(IO.Seekable io, List<RangeLong> fragments, boolean closeParentIOOnClose, String description) {
		this.io = io;
		this.fragments = fragments;
		this.closeParentIOOnClose = closeParentIOOnClose;
		this.description = description;
		size = 0;
		for (RangeLong r : fragments) size += r.max - r.min + 1;
	}
	
	protected IO.Seekable io;
	protected List<RangeLong> fragments;
	protected long pos = 0;
	protected long size;
	protected boolean closeParentIOOnClose;
	protected String description;

	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		if (!closeParentIOOnClose) return null;
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		fragments = null;
		ondone.unblock();
	}

	/** Readable fragmented IO. */
	public static class Readable extends FragmentedSubIO implements IO.Readable.Seekable {
		
		/** Constructor. */
		public Readable(IO.Readable.Seekable io, List<RangeLong> fragments, boolean closeParentIOOnClose, String description) {
			super(io, fragments, closeParentIOOnClose, description);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return ((IO.Readable.Seekable)io).canStartReading();
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
		) {
			return super.readAsync(pos, buffer, ondone);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			int nb = super.readSync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			return super.readSync(pos, buffer);
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			return super.readFullySync(pos, buffer);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			int nb = super.readFullySync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return readFullyAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readFullyAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
		) {
			return operation(IOUtil.readFullyAsync(this, pos, buffer, ondone));
		}
		
		@Override
		public long skipSync(long n) {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return super.skipAsync(n, ondone);
		}
	}
	
	/** Readable and Writable fragmented IO. */
	public static class ReadWrite extends FragmentedSubIO.Readable implements IO.Writable.Seekable {
		
		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable> ReadWrite(
			T io, List<RangeLong> fragments, boolean closeParentIOOnClose, String description
		) {
			super(io, fragments, closeParentIOOnClose, description);
		}

		@Override
		public IAsync<IOException> canStartWriting() {
			return super.canStartWriting();
		}

		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}

		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			int nb = super.writeSync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}

		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
		) {
			return super.writeAsync(pos, buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.writeAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
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
	public Priority getPriority() {
		return io.getPriority();
	}
	
	@Override
	public void setPriority(Priority priority) {
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
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		AsyncSupplier<Long, IOException> sp = new AsyncSupplier<>();
		sp.unblockSuccess(Long.valueOf(getSizeSync()));
		return sp;
	}
	
	// Readable
	
	protected AsyncSupplier<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
				return ((IO.Readable.Seekable)io).readAsync(r.min + start, buffer, param -> {
					buffer.limit(prevLimit);
					if (ondone != null) ondone.accept(param);
				});
			}
			return operation(((IO.Readable.Seekable)io).readAsync(r.min + start, buffer, ondone));
		}
		AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
		if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(0), null));
		sp.unblockSuccess(Integer.valueOf(0));
		return sp;
	}
	
	protected int readSync(long pos, ByteBuffer buffer) throws IOException {
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
				len = ((IO.Readable.Seekable)io).readSync(r.min + start, buffer);
				buffer.limit(prevLimit);
			} else {
				len = ((IO.Readable.Seekable)io).readSync(r.min + start, buffer);
			}
			return len;
		}
		return 0;
	}
	
	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
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
				len = ((IO.Readable.Seekable)io).readFullySync(r.min + start, buffer);
				buffer.limit(prevLimit);
			} else {
				len = ((IO.Readable.Seekable)io).readFullySync(r.min + start, buffer);
			}
			done += len;
			if (!buffer.hasRemaining()) return done;
			// continue on next fragment
			pos += len;
			p += s;
		}
		return done;
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
		default: //case FROM_CURRENT:
			pos += move;
			break;
		}
		if (pos > size) pos = size;
		if (pos < 0) pos = 0;
		return pos;
	}
	
	protected long skipSync(long n) {
		long siz = getSizeSync();
		long prevPos = pos;
		pos += n;
		if (pos > siz) pos = siz;
		if (pos < 0) pos = 0;
		return pos - prevPos;
	}
	
	@Override
	public AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		AsyncSupplier<Long,IOException> sp = new AsyncSupplier<>();
		seekSync(type, move);
		if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(pos), null));
		sp.unblockSuccess(Long.valueOf(pos));
		return sp;
	}
	
	protected AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		AsyncSupplier<Long,IOException> sp = new AsyncSupplier<>();
		long skipped = skipSync(n);
		if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(skipped), null));
		sp.unblockSuccess(Long.valueOf(skipped));
		return sp;
	}
	
	// Writable
	
	protected IAsync<IOException> canStartWriting() {
		return ((IO.Writable)io).canStartWriting();
	}

	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		Iterator<RangeLong> it = fragments.iterator();
		long p = 0;
		int total = 0;
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
				len = ((IO.Writable.Seekable)io).writeSync(r.min + start, buffer);
				buffer.limit(prevLimit);
			} else {
				len = ((IO.Writable.Seekable)io).writeSync(r.min + start, buffer);
			}
			total += len;
			pos += len;
			p += s;
			if (!buffer.hasRemaining())
				return total;
		}
		return total;
	}

	protected AsyncSupplier<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		Iterator<RangeLong> it = fragments.iterator();
		long p = 0;
		while (it.hasNext()) {
			RangeLong r = it.next();
			long s = r.max - r.min + 1;
			if (pos >= p + s) {
				p += s;
				continue;
			}
			AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
			writeAsync(it, r, p, 0, pos, buffer, ondone, sp);
			return operation(sp);
		}
		AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
		if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(0), null));
		sp.unblockSuccess(Integer.valueOf(0));
		return sp;
	}
	
	@SuppressWarnings("squid:S00107")
	protected void writeAsync(
		Iterator<RangeLong> it, RangeLong r, long p, int done, long pos,
		ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone, AsyncSupplier<Integer,IOException> sp
	) {
		long start = pos - p;
		int len = buffer.remaining();
		long s = r.max - r.min + 1;
		if (start + len > s) {
			int prevLimit = buffer.limit();
			buffer.limit((int)(prevLimit - ((start + len) - s)));
			IOUtil.listenOnDone(((IO.Writable.Seekable)io).writeAsync(r.min + start, buffer), nb -> {
				buffer.limit(prevLimit);
				int i = nb.intValue();
				if (!buffer.hasRemaining() || !it.hasNext()) {
					IOUtil.success(Integer.valueOf(i), sp, ondone);
					return;
				}
				writeAsync(it, it.next(), p + s, done + i, pos + i, buffer, ondone, sp);
			}, sp, ondone);
			return;
		}
		IOUtil.listenOnDone(((IO.Writable.Seekable)io).writeAsync(r.min + start, buffer),
			nb -> IOUtil.success(Integer.valueOf(nb.intValue() + done), sp, ondone), sp, ondone);
	}
	
}
