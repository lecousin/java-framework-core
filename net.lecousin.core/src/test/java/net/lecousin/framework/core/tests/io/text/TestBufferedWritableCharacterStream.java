package net.lecousin.framework.core.tests.io.text;

import java.nio.charset.Charset;

import net.lecousin.framework.core.test.io.TestCharacterStreamWritable;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

public class TestBufferedWritableCharacterStream extends TestCharacterStreamWritable {

	@Override
	protected ICharacterStream.Writable open(IO.Writable out, Charset charset) {
		return new BufferedWritableCharacterStream(out, charset, 5);
	}
	
	@Override
	protected void flush(ICharacterStream.Writable cs) throws Exception {
		((BufferedWritableCharacterStream)cs).flush().blockThrow(0);
	}
	
}
