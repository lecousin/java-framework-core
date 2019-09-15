package net.lecousin.framework.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.FileAccess;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * IO on a file, using one of the 3 sub-classes: FileIO.ReadOnly, FileIO.WriteOnly, FileIO.ReadWrite.
 */
public abstract class FileIO extends ConcurrentCloseable implements IO.KnownSize {

	protected FileIO(File file, String mode, byte priority) {
		this.file = new FileAccess(file, mode, priority);
		this.file.getStartingTask().start();
	}
	
	protected FileAccess file;
	protected long position = 0;
	
	/** Can start reading or writing. */
	public ISynchronizationPoint<IOException> canStart() {
		return file.getStartingTask().getOutput();
	}
	
	public File getFile() {
		return file.getFile();
	}
	
	@Override
	public String getSourceDescription() {
		return file.getPath();
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}
	
	@Override
	public byte getPriority() {
		return file.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		file.setPriority(priority);
	}
	
	@Override
	public TaskManager getTaskManager() {
		return Threading.getDrivesTaskManager().getTaskManager(file.getFile());
	}
	
	/** FileIO in read-only mode. */
	public static class ReadOnly extends FileIO implements IO.Readable.Seekable {
		/** Constructor. */
		public ReadOnly(File file, byte priority) {
			super(file, "r", priority);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return canStart();
		}
		
		@Override
		public long getPosition() {
			return super.getPosition();
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return super.skipAsync(n, ondone);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return super.readSync(buffer);
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			return super.readSync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buffer, ondone);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			return super.readFullySync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(pos, buffer, ondone);
		}
	}
	
	/** FileIO in write-only mode. */
	public static class WriteOnly extends FileIO implements IO.Writable.Seekable, IO.Resizable {
		
		/** Constructor. */
		public WriteOnly(File file, byte priority) {
			super(file, "rw", priority);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartWriting() {
			return canStart();
		}
		
		@Override
		public long getPosition() {
			return super.getPosition();
		}
		
		@Override
		public void setSizeSync(long size) throws IOException {
			super.setSizeSync(size);
		}
		
		@Override
		public AsyncWork<Void, IOException> setSizeAsync(long size) {
			return super.setSizeAsync(size);
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			return super.writeSync(buffer);
		}
		
		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(pos, buffer, ondone);
		}
	}
	
	/** FileIO with read and write operations. */
	public static class ReadWrite extends FileIO implements IO.Readable.Seekable, IO.Writable.Seekable, IO.Resizable, IO.KnownSize {
		
		/** Constructor. */
		public ReadWrite(File file, byte priority) {
			super(file, "rw", priority);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return canStart();
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartWriting() {
			return canStart();
		}
		
		@Override
		public long getPosition() {
			return super.getPosition();
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return super.skipAsync(n, ondone);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return super.readSync(buffer);
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			return super.readSync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buffer, ondone);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			return super.readFullySync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(pos, buffer, ondone);
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			return super.writeSync(buffer);
		}
		
		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}
		
		@Override
		public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(pos, buffer, ondone);
		}

		@Override
		public void setSizeSync(long newSize) throws IOException {
			super.setSizeSync(newSize);
		}
		
		@Override
		public AsyncWork<Void, IOException> setSizeAsync(long newSize) {
			return super.setSizeAsync(newSize);
		}
	}
	
	protected long getPosition() {
		return position;
	}
	
	protected long seekSync(SeekType type, long move) throws IOException {
		return position = file.seek(type, move, false);
	}
	
	protected AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return operation(file.seekAsync(type, move, false, (res) -> {
			if (res.getValue1() != null)
				position = res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		}).getOutput());
	}
	
	protected long skipSync(long n) throws IOException {
		long change = file.skip(n);
		position += change;
		return change;
	}
	
	protected AsyncWork<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return operation(file.skipAsync(n, (res) -> {
			if (res.getValue1() != null)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		}).getOutput());
	}
	
	@Override
	public long getSizeSync() throws IOException {
		return file.getSize();
	}
	
	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		AsyncWork<Long, IOException> sp = new AsyncWork<>();
		file.getSize(sp);
		return operation(sp);
	}
	
	protected void setSizeSync(long size) throws IOException {
		file.setSize(size);
		if (position > size) position = size;
	}
	
	protected AsyncWork<Void,IOException> setSizeAsync(long size) {
		if (position > size) position = size;
		return operation(file.setSizeAsync(size).getOutput());
	}
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		file.closeAsync().getOutput().listenInlineSP(ondone);
	}
	
	protected int readSync(ByteBuffer buffer) throws IOException {
		int nb = file.read(position, buffer);
		if (nb > 0) position += nb;
		return nb;
	}
	
	protected int readSync(long pos, ByteBuffer buffer) throws IOException {
		return file.read(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.readAsync(position, buffer, (res) -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		}).getOutput());
	}
	
	protected AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.readAsync(pos, buffer, ondone).getOutput());
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = file.readFully(position, buffer);
		if (nb > 0) position += nb;
		return nb;
	}
	
	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return file.readFully(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.readFullyAsync(position, buffer, (res) -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		}).getOutput());
	}
	
	protected AsyncWork<Integer,IOException>
	readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.readFullyAsync(pos, buffer, ondone).getOutput());
	}
	
	protected int writeSync(ByteBuffer buffer) throws IOException {
		int nb = file.write(position, buffer);
		if (nb > 0) position += nb;
		return nb;

	}
	
	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		return file.write(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.writeAsync(position, buffer, (res) -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		}).getOutput());
	}
	
	protected AsyncWork<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(file.writeAsync(pos, buffer, ondone).getOutput());
	}

}
