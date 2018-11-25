package net.lecousin.framework.core.test.io;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.Task;
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
	
	@Test(timeout=120000)
	public void test() throws Exception {
		testWrite("Hello", StandardCharsets.US_ASCII);
		testWrite("Bonjour vous-même", StandardCharsets.UTF_8);
		testWrite("Hello World !", StandardCharsets.UTF_16);
	}
	
	@SuppressWarnings("resource")
	private void testWrite(String s, Charset charset) throws Exception {
		File tmp = TemporaryFiles.get().createFileSync("test", "writablecs");
		ICharacterStream.Writable cs = open(new FileIO.WriteOnly(tmp, Task.PRIORITY_NORMAL), charset);
		// basic tests
		cs.setPriority(Task.PRIORITY_IMPORTANT);
		Assert.assertEquals(Task.PRIORITY_IMPORTANT, cs.getPriority());
		cs.getDescription();
		Assert.assertEquals(charset, cs.getEncoding());
		// write
		cs.writeSync(s);
		flush(cs);
		cs.close();
		Assert.assertEquals(s, IOUtil.readFullyAsStringSync(tmp, charset));
		tmp.delete();

		tmp = TemporaryFiles.get().createFileSync("test", "writablecs");
		cs = open(new FileIO.WriteOnly(tmp, Task.PRIORITY_NORMAL), charset);
		cs.writeAsync(s).blockThrow(0);
		flush(cs);
		cs.close();
		Assert.assertEquals(s, IOUtil.readFullyAsStringSync(tmp, charset));
		tmp.delete();
	}
	
}
