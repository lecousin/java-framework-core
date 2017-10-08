package net.lecousin.framework.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.FileAccess;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * IO on a file, using one of the 3 sub-classes: FileIO.ReadOnly, FileIO.WriteOnly, FileIO.ReadWrite.
 */
public abstract class FileIO extends IO.AbstractIO implements IO.KnownSize {

	protected FileIO(File file, String mode, byte priority) {
		this.file = new FileAccess(file, mode, priority);
		this.file.getStartingTask().start();
	}
	
	protected FileAccess file;
	
	/** Can start reading or writing. */
	public ISynchronizationPoint<IOException> canStart() {
		return file.getStartingTask().getSynch();
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
		public long getPosition() throws IOException {
			return super.getPosition();
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(pos, buffer, ondone);
		}
	}
	
	/** FileIO in write-only mode. */
	public static class WriteOnly extends FileIO implements IO.Writable.Seekable {
		
		/** Constructor. */
		public WriteOnly(File file, byte priority) {
			super(file, "rw", priority);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartWriting() {
			return canStart();
		}
		
		@Override
		public long getPosition() throws IOException {
			return super.getPosition();
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		public long getPosition() throws IOException {
			return super.getPosition();
		}
		
		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException>
		writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
	
	protected long getPosition() throws IOException {
		return file.getPosition();
	}
	
	protected long seekSync(SeekType type, long move) throws IOException {
		return file.seek(type, move, false);
	}
	
	protected AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return file.seekAsynch(type, move, false, ondone).getSynch();
	}
	
	protected long skipSync(long n) throws IOException {
		return file.skip(n);
	}
	
	protected AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return file.skipAsynch(n, ondone).getSynch();
	}
	
	@Override
	public long getSizeSync() throws IOException {
		return file.getSize();
	}
	
	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		AsyncWork<Long, IOException> sp = new AsyncWork<>();
		file.getSize(sp);
		return sp;
	}
	
	protected void setSizeSync(long size) throws IOException {
		file.setSize(size);
	}
	
	protected AsyncWork<Void,IOException> setSizeAsync(long size) {
		return file.setSizeAsynch(size).getSynch();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		return file.closeAsynch().getSynch();
	}
	
	protected int readSync(ByteBuffer buffer) throws IOException {
		return file.read(-1, buffer);
	}
	
	protected int readSync(long pos, ByteBuffer buffer) throws IOException {
		return file.read(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.readAsynch(-1, buffer, ondone).getSynch();
	}
	
	protected AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.readAsynch(pos, buffer, ondone).getSynch();
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		return file.readFully(-1, buffer);
	}
	
	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return file.readFully(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.readFullyAsynch(-1, buffer, ondone).getSynch();
	}
	
	protected AsyncWork<Integer,IOException>
	readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.readFullyAsynch(pos, buffer, ondone).getSynch();
	}
	
	protected int writeSync(ByteBuffer buffer) throws IOException {
		return file.write(-1, buffer);
	}
	
	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		return file.write(pos, buffer);
	}
	
	protected AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.writeAsynch(-1, buffer, ondone).getSynch();
	}
	
	protected AsyncWork<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return file.writeAsynch(pos, buffer, ondone).getSynch();
	}

}
