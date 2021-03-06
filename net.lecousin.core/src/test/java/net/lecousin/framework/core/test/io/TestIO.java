package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.CloseableListenable;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestIO extends LCCoreAbstractTest {
	
	public static abstract class UsingTestData extends TestIO {

		public UsingTestData(byte[] testBuf, int nbBuf) {
			this.testBuf = testBuf;
			this.nbBuf = nbBuf;
		}
		
		protected byte[] testBuf;
		protected int nbBuf;
		
		private static final byte[] testBuf1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\nabcdefghijklmnopqrstuvwxyz\r\n0123456789012345678901234567890123456789\r\n\r\n".getBytes();
		private static final int nbBuf1Large = 100000;
		private static final int nbBuf1Large_faster = 60000;
		private static final int nbBuf1Medium = 1000;
		private static final int nbBuf1Medium_faster = 800;
		private static final int nbBuf1Small = 100;
		private static final int nbBuf1Tiny = 1;
		private static final int nbBuf1Empty = 0;
		
		public static List<Object[]> generateTestCases(boolean faster) {
			LinkedList<Object[]> list = new LinkedList<>();
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Empty) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Tiny) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Small) });
			list.add(new Object[] { testBuf1, Integer.valueOf(faster ? nbBuf1Medium_faster : nbBuf1Medium) });
			list.add(new Object[] { testBuf1, Integer.valueOf(faster ? nbBuf1Large_faster : nbBuf1Large) });
			return list;
		}
		
	}
	
	public static abstract class UsingGeneratedTestFiles extends UsingTestData {
		
		public UsingGeneratedTestFiles(File testFile, byte[] testBuf, int nbBuf) {
			super(testBuf, nbBuf);
			this.testFile = testFile;
		}
		
		private static File[] files_normal;
		private static File[] files_faster;

		protected File testFile;
		
		public static synchronized List<Object[]> generateTestCases(boolean faster) {
			List<Object[]> base = UsingTestData.generateTestCases(faster);
			File[] files = faster ? files_faster : files_normal;
			if (files == null) {
				files = new File[base.size()];
				for (int i = 0; i < files.length; ++i) {
					Object[] params = base.get(i);
					try { files[i] = generateFile((byte[])params[0], ((Integer)params[1]).intValue()); }
					catch (IOException e) {
						throw new RuntimeException("Error generating file", e);
					}
				}
				if (faster) files_faster = files; else files_normal = files;
			}
			LinkedList<Object[]> list = new LinkedList<>();
			for (int i = 0; i < files.length; ++i) {
				Object[] params = base.get(i);
				list.add(new Object[] { files[i], params[0], params[1] });
			}
			return list;
		}
		
		protected static File generateFile(byte[] testBuf, int nbBuf) throws IOException {
			System.out.println("Generating file of "+nbBuf+" * "+testBuf.length+" = "+(nbBuf*testBuf.length));
			File file = File.createTempFile("test", "lcfw");
			file.deleteOnExit();
			FileOutputStream out = new FileOutputStream(file);
			for (int i = 0; i < nbBuf; ++i)
				out.write(testBuf);
			out.flush();
			out.close();
			return file;
		}
		
		protected FileIO.ReadOnly openFile() {
			return new FileIO.ReadOnly(testFile, Task.Priority.IMPORTANT);
		}
		
		protected long getFileSize() {
			return ((long)testBuf.length) * nbBuf;
		}
	}

	protected static final Random rand = new Random();
	
	protected abstract IO getIOForCommonTests() throws Exception;
	
	@Test
	public void testBasicCommonFunctions() throws Exception {
		IO io = getIOForCommonTests();
		basicTests(io);
		// closeable
		MutableBoolean closed = new MutableBoolean(false);
		MutableBoolean closed2 = new MutableBoolean(false);
		Runnable listener1 = () -> {};
		Consumer<CloseableListenable> listener2 = (toto) -> {};
		io.addCloseListener(() -> {
			closed.set(true);
		});
		io.addCloseListener((toto) -> {
			closed2.set(true);
		});
		io.addCloseListener(listener1);
		io.addCloseListener(listener2);
		io.lockClose();
		IAsync<IOException> close = io.closeAsync();
		Assert.assertFalse(io.isClosed());
		Assert.assertFalse(close.isDone());
		Assert.assertFalse(closed.get());
		Assert.assertFalse(closed2.get());
		io.removeCloseListener(listener1);
		io.removeCloseListener(listener2);
		io.unlockClose();
		close.blockThrow(0);
		Assert.assertTrue(closed.get());
		Assert.assertTrue(closed2.get());
		Assert.assertTrue(io.isClosed());

		closed.set(false);
		closed2.set(false);
		io.addCloseListener(() -> {
			closed.set(true);
		});
		Assert.assertTrue(closed.get());
		Assert.assertFalse(closed2.get());
		io.addCloseListener((toto) -> {
			closed2.set(true);
		});
		Assert.assertTrue(closed2.get());
	}

	protected boolean canSetPriority() { return true; }
	
	protected void basicTests(IO io) throws Exception {
		// priority
		if (canSetPriority()) {
			Priority p = io.getPriority();
			io.setPriority(Task.Priority.LOW);
			Assert.assertEquals(Task.Priority.LOW, io.getPriority());
			io.setPriority(p);
			Assert.assertEquals(p, io.getPriority());
		}
		// basic functions, only for test coverage
		io.getTaskManager();
		io.getSourceDescription();
		io.getWrappedIO();
	}

}
