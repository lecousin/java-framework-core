package net.lecousin.framework.core.tests.io.text;

import java.nio.charset.Charset;

import net.lecousin.framework.core.test.io.TestCharacterStreamWritable;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.io.text.WritableCharacterStream;

public class TestWritableCharacterStream extends TestCharacterStreamWritable {

	@Override
	protected ICharacterStream.Writable open(IO.Writable out, Charset charset) {
		return new WritableCharacterStream(out, charset);
	}
	
	@Override
	protected void flush(ICharacterStream.Writable cs) {
	}

}
