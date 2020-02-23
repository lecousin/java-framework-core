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

public abstract class TestCharacterStreamWritableBuffered extends LCCoreAbstractTest {

	protected abstract ICharacterStream.Writable.Buffered open(IO.Writable out, Charset charset) throws Exception;
	
	protected abstract void flush(ICharacterStream.Writable cs) throws Exception;
	
	@Test
	public void test() throws Exception {
		testWrite("Hello", StandardCharsets.US_ASCII);
		testWrite("Bonjour vous-même", StandardCharsets.UTF_8);
		testWrite("Hello World !", StandardCharsets.UTF_16);
	}
	
	private void testWrite(String s, Charset charset) throws Exception {
		File tmp = TemporaryFiles.get().createFileSync("test", "writablecs");
		ICharacterStream.Writable.Buffered cs = open(new FileIO.WriteOnly(tmp, Task.Priority.NORMAL), charset);
		char[] chars = s.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			if ((i % 2) == 0)
				cs.writeSync(chars[i]);
			else
				cs.writeAsync(chars[i]).blockThrow(0);
		}
		flush(cs);
		cs.close();
		Assert.assertEquals(s, IOUtil.readFullyAsStringSync(tmp, charset));
		tmp.delete();
	}
	
}
