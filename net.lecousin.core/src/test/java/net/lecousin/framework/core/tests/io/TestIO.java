package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

public abstract class TestIO extends LCCoreAbstractTest {
	
	public static abstract class UsingTestData extends TestIO {

		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public UsingTestData(byte[] testBuf, int nbBuf) {
			this.testBuf = testBuf;
			this.nbBuf = nbBuf;
		}
		
		protected byte[] testBuf;
		protected int nbBuf;
		
		private static final byte[] testBuf1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\nabcdefghijklmnopqrstuvwxyz\r\n0123456789012345678901234567890123456789\r\n\r\n".getBytes();;
		private static final int nbBuf1Large = 100000;
		private static final int nbBuf1Medium = 1000;
		private static final int nbBuf1Small = 100;
		private static final int nbBuf1Tiny = 1;
		private static final int nbBuf1Empty = 0;
		
		protected static List<Object[]> generateTestCases() {
			LinkedList<Object[]> list = new LinkedList<>();
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Empty) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Tiny) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Small) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Medium) });
			list.add(new Object[] { testBuf1, Integer.valueOf(nbBuf1Large) });
			return list;
		}
		
	}
	
	public static abstract class UsingGeneratedTestFiles extends UsingTestData {
		
		public UsingGeneratedTestFiles(File testFile, byte[] testBuf, int nbBuf) {
			super(testBuf, nbBuf);
			this.testFile = testFile;
		}
		
		private static File[] files;

		protected File testFile;
		
		protected static synchronized List<Object[]> generateTestCases() {
			List<Object[]> base = UsingTestData.generateTestCases();
			if (files == null) {
				files = new File[base.size()];
				for (int i = 0; i < files.length; ++i) {
					Object[] params = base.get(i);
					try { files[i] = generateFile((byte[])params[0], ((Integer)params[1]).intValue()); }
					catch (IOException e) {
						throw new RuntimeException("Error generating file", e);
					}
				}
			}
			LinkedList<Object[]> list = new LinkedList<>();
			for (int i = 0; i < files.length; ++i) {
				Object[] params = base.get(i);
				list.add(new Object[] { files[i], params[0], params[1] });
			}
			return list;
		}
		
		private static File generateFile(byte[] testBuf, int nbBuf) throws IOException {
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
			return new FileIO.ReadOnly(testFile, Task.PRIORITY_IMPORTANT);
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
		io.close();
	}

	protected boolean canSetPriority() { return true; }
	
	protected void basicTests(IO io) throws Exception {
		// priority
		if (canSetPriority()) {
			byte p = io.getPriority();
			io.setPriority(Task.PRIORITY_LOW);
			Assert.assertEquals(Task.PRIORITY_LOW, io.getPriority());
			io.setPriority(p);
			Assert.assertEquals(p, io.getPriority());
		}
		// basic functions, only for test coverage
		io.getTaskManager();
		io.getSourceDescription();
		io.getWrappedIO();
	}

	/* TODO
canStartWriting

SizeKnown:
 - getSizeAsync
 
Resizable:
 - setSize with same size
 
Buffered:
 - skips
 - skip(int) that goes before the beginning
 
start to read quickly so bufferization is not yet done (use a task to avoid disk access for few seconds?)

TwoBuffersIO.DeterminedSize
BufferedIO: increaseSize at the beginning
BufferedIO.flush
BufferedIO.getSizeSync
BufferedIO.ReadWrite.skipAsync
BufferedIO.ReadWrite.readNextBufferAsync

write(byte)
write(byte[], int, int)
setSize with much larger size

test generating an error while reading ?
	 */
}
