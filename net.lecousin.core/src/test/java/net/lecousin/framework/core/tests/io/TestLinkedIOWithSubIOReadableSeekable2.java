package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.core.test.io.TestFragmented;
import net.lecousin.framework.core.test.io.TestFragmented.FragmentedFile;
import net.lecousin.framework.core.test.io.TestReadableSeekable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.LinkedIO;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.BufferedIO;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.util.CloseableListenable;
import net.lecousin.framework.util.Pair;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestLinkedIOWithSubIOReadableSeekable2 extends TestReadableSeekable {

	@Parameters
	public static Collection<Object[]> parameters() throws IOException {
		return TestFragmented.generateTestCases();
	}
	
	public TestLinkedIOWithSubIOReadableSeekable2(FragmentedFile f) {
		super(f.file, f.testBuf, f.nbBuf);
		this.f = f;
	}
	
	private FragmentedFile f;

	@Override
	protected IO.Readable.Seekable createReadableSeekableFromFile(FileIO.ReadOnly file, long fileSize) throws Exception {
		// this test may be very slow, let's add a buffered layer
		BufferedIO buffered = new BufferedIO(file, f.realSize, 32768, 32768, false);
		IO.Readable.Seekable[] ios = new IO.Readable.Seekable[f.fragments.size()];
		int i = 0;
		for (RangeLong fragment : f.fragments)
			ios[i++] = new SizeNotKnown(new SubIO.Readable.Seekable(buffered, fragment.min, fragment.getLength(), "fragment " + i, false));
		LinkedIO.Readable.Seekable res = new LinkedIO.Readable.Seekable.DeterminedSize("linked IO", ios);
		res.addCloseListener(() -> { try { buffered.close(); } catch (Exception e) {}});
		return res;
	}
	
	@Override
	protected boolean canSetPriority() {
		return !f.fragments.isEmpty() && f.nbBuf > 0;
	}

	
	public static class SizeNotKnown implements IO.Readable.Seekable {
		public SizeNotKnown(IO.Readable.Seekable io) {
			this.io = io;
		}
		
		private IO.Readable.Seekable io;

		@Override
		public boolean lockClose() {
			return io.lockClose();
		}

		@Override
		public boolean isClosed() {
			return io.isClosed();
		}

		@Override
		public void unlockClose() {
			io.unlockClose();
		}

		@Override
		public void addCloseListener(Consumer<CloseableListenable> listener) {
			io.addCloseListener(listener);
		}

		@Override
		public void addCloseListener(Runnable listener) {
			io.addCloseListener(listener);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return io.canStartReading();
		}

		@Override
		public void close() throws Exception {
			io.close();
		}

		@Override
		public IAsync<IOException> closeAsync() {
			return io.closeAsync();
		}

		@Override
		public String getSourceDescription() {
			return io.getSourceDescription();
		}

		@Override
		public void removeCloseListener(Consumer<CloseableListenable> listener) {
			io.removeCloseListener(listener);
		}

		@Override
		public IO getWrappedIO() {
			return io.getWrappedIO();
		}

		@Override
		public void removeCloseListener(Runnable listener) {
			io.removeCloseListener(listener);
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
		public TaskManager getTaskManager() {
			return io.getTaskManager();
		}

		@Override
		public long getPosition() throws IOException {
			return io.getPosition();
		}

		@Override
		public long seekSync(SeekType type, long move) throws IOException {
			return io.seekSync(type, move);
		}

		@Override
		public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move,
				Consumer<Pair<Long, IOException>> ondone) {
			return io.seekAsync(type, move, ondone);
		}

		@Override
		public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move) {
			return io.seekAsync(type, move);
		}

		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return io.readSync(buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer,
				Consumer<Pair<Integer, IOException>> ondone) {
			return io.readAsync(buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer) {
			return io.readAsync(buffer);
		}

		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return io.readFullySync(buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer,
				Consumer<Pair<Integer, IOException>> ondone) {
			return io.readFullyAsync(buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer) {
			return io.readFullyAsync(buffer);
		}

		@Override
		public long skipSync(long n) throws IOException {
			return io.skipSync(n);
		}

		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return io.skipAsync(n, ondone);
		}

		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n) {
			return io.skipAsync(n);
		}

		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			return io.readSync(pos, buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer,
				Consumer<Pair<Integer, IOException>> ondone) {
			return io.readAsync(pos, buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer) {
			return io.readAsync(pos, buffer);
		}

		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			return io.readFullySync(pos, buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer,
				Consumer<Pair<Integer, IOException>> ondone) {
			return io.readFullyAsync(pos, buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer) {
			return io.readFullyAsync(pos, buffer);
		}
		
	}

}
