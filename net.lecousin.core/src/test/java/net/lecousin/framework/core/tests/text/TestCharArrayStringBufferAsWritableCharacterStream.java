package net.lecousin.framework.core.tests.text;

import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.io.TestCharacterStreamWritable;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.text.CharArrayStringBuffer;

public class TestCharArrayStringBufferAsWritableCharacterStream extends TestCharacterStreamWritable {

	private CharArrayStringBuffer c;
	private  IO.Writable out;
	private Charset charset;
	
	@Override
	protected ICharacterStream.Writable open(IO.Writable out, Charset charset) throws Exception {
		c = new CharArrayStringBuffer();
		this.out = out;
		this.charset = charset;
		ICharacterStream.Writable cs = c.asWritableCharacterStream();
		cs.addCloseListener(() -> {
			try { out.close(); }
			catch (Exception e) {}
		});
		return cs;
	}

	@Override
	protected void flush(ICharacterStream.Writable cs) throws Exception {
		c.encode(charset, out, Priority.NORMAL).blockThrow(0);
	}
	
}
