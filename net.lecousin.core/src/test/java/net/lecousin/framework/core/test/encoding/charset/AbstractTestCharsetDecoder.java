package net.lecousin.framework.core.test.encoding.charset;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.charset.CharacterDecoder;
import net.lecousin.framework.encoding.charset.CharacterDecoderFromCharsetDecoder;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.Chars.Readable;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.text.CharArrayString;

import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestCharsetDecoder extends LCCoreAbstractTest {
	
	protected Charset charset;
	
	protected AbstractTestCharsetDecoder(Charset charset) {
		this.charset = charset;
	}
	
	protected abstract String[] getTestStrings();
	
	@Test
	public void testStrings() {
		for (String s : getTestStrings())
			try { test(s, charset); }
			catch (Throwable e) {
				throw new AssertionError("Error with string: " + s, e);
			}
	}

	public static void test(String s, Charset charset) throws Exception {
		test(s, charset, 4096);
		test(s, charset, 16);
		test(s, charset, 8);
		test(s, charset, 4);
		//test(s, charset, 2);
	}
	
	public static void test(String s, Charset charset, int bufferSize) throws Exception {
		CharacterDecoder decoder = CharacterDecoder.get(charset, bufferSize);
		test(s, decoder);
		if (!(decoder instanceof CharacterDecoderFromCharsetDecoder))
			test(s, new CharacterDecoderFromCharsetDecoder(charset.newDecoder(), bufferSize));
	}
	
	public static void test(String s, CharacterDecoder decoder) throws Exception {
		byte[] bytes = s.getBytes(decoder.getEncoding());
		Chars.Readable chars = decoder.decode(new RawByteBuffer(bytes));
		Chars.Readable end = decoder.flush();
		if (end != null)
			chars = new CompositeChars.Readable(chars, end);
		check(s, chars);
		
		// byte by byte
		CompositeChars.Readable composite = new CompositeChars.Readable();
		for (int i = 0; i < bytes.length; ++i) {
			chars = decoder.decode(new RawByteBuffer(bytes, i, 1));
			if (chars.hasRemaining())
				composite.add(chars);
		}
		end = decoder.flush();
		if (end != null)
			composite.add(end);
		check(s, composite);
		
		// by 3 bytes
		composite = new CompositeChars.Readable();
		for (int i = 0; i < bytes.length / 3; ++i) {
			chars = decoder.decode(new RawByteBuffer(bytes, i * 3, 3));
			if (chars.hasRemaining())
				composite.add(chars);
		}
		if ((bytes.length % 3) > 0) {
			chars = decoder.decode(new RawByteBuffer(bytes, (bytes.length / 3) * 3, bytes.length % 3));
			if (chars.hasRemaining())
				composite.add(chars);
		}
		end = decoder.flush();
		if (end != null)
			composite.add(end);
		check(s, composite);
		
		CharArrayString str = new CharArrayString(256);
		// using consumer
		decoder.createConsumer(new AsyncConsumer<Chars.Readable, IOException>() {
			@Override
			public IAsync<IOException> consume(Chars.Readable data, Consumer<Readable> onDataRelease) {
				data.get(str, data.remaining());
				return new Async<>(true);
			}
			
			@Override
			public IAsync<IOException> end() {
				return new Async<>(true);
			}
			
			@Override
			public void error(IOException error) {
			}
		}).consumeEnd(new RawByteBuffer(bytes), null).blockThrow(0);
		Assert.assertEquals(s, str.asString());
		
		StringBuilder sb = new StringBuilder();
		decoder.decodeConsumerToString(res -> sb.append(res)).consumeEnd(new RawByteBuffer(bytes), null).blockThrow(0);
		Assert.assertEquals(s, sb.toString());
	}
	
	private static void check(String s, Chars.Readable chars) {
		Assert.assertEquals("Length", s.length(), chars.remaining());
		for (int i = 0; i < s.length(); ++i) {
			Assert.assertTrue(chars.hasRemaining());
			Assert.assertEquals("Char " + i, s.charAt(i), chars.get());
		}
	}

	
}
