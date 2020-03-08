package net.lecousin.framework.core.test.io;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestCharacterStreamWritable extends LCCoreAbstractTest {

	protected abstract ICharacterStream.Writable open(IO.Writable out, Charset charset) throws Exception;
	
	protected abstract void flush(ICharacterStream.Writable cs) throws Exception;
	
	@Test
	public void test() throws Exception {
		testWrite("Hello", StandardCharsets.US_ASCII);
		testWrite("Bonjour vous-même", StandardCharsets.UTF_8);
		testWrite("Hello World !", StandardCharsets.UTF_16);
	}
	
	private void testWrite(String s, Charset charset) throws Exception {
		File tmp = TemporaryFiles.get().createFileSync("test", "writablecs");
		ICharacterStream.Writable cs = open(new FileIO.WriteOnly(tmp, Task.Priority.NORMAL), charset);
		// basic tests
		cs.setPriority(Task.Priority.IMPORTANT);
		Assert.assertEquals(Task.Priority.IMPORTANT, cs.getPriority());
		cs.getDescription();
		cs.getEncoding();
		// write
		cs.writeSync(s);
		flush(cs);
		cs.close();
		Assert.assertEquals(s, IOUtil.readFullyAsStringSync(tmp, charset));
		tmp.delete();

		tmp = TemporaryFiles.get().createFileSync("test", "writablecs");
		cs = open(new FileIO.WriteOnly(tmp, Task.Priority.NORMAL), charset);
		cs.writeAsync(s).blockThrow(0);
		flush(cs);
		cs.close();
		Assert.assertEquals(s, IOUtil.readFullyAsStringSync(tmp, charset));
		tmp.delete();
	}
	
}
