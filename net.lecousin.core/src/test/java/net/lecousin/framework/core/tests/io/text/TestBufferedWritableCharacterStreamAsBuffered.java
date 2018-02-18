package net.lecousin.framework.core.tests.io.text;

import java.nio.charset.Charset;

import net.lecousin.framework.core.test.io.TestCharacterStreamWritableBuffered;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

public class TestBufferedWritableCharacterStreamAsBuffered extends TestCharacterStreamWritableBuffered {

	@Override
	protected ICharacterStream.Writable.Buffered open(Writable out, Charset charset) {
		return new BufferedWritableCharacterStream(out, charset, 5);
	}
	
	@Override
	protected void flush(ICharacterStream.Writable cs) throws Exception {
		((BufferedWritableCharacterStream)cs).flush().blockThrow(0);
	}
	
}
