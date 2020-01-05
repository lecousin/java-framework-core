package net.lecousin.framework.core.tests.encoding.charset;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.charset.CharacterDecoder;
import net.lecousin.framework.encoding.charset.CharacterDecoderFromCharsetDecoder;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.RawByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class TestDecoders extends LCCoreAbstractTest {

	@Test
	public void testUSASCII_1() {
		test("abcd012GH)I_V;", StandardCharsets.US_ASCII);
	}

	@Test
	public void testISO8859_1() {
		test("abcd012GH)I_V;", StandardCharsets.ISO_8859_1);
	}

	@Test
	public void testUTF8_1() {
		test("abcd012GH)I_V;", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_2() {
		test("И вдаль глядел. Пред ним широко", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_3() {
		test("Μονάχη ἔγνοια ἡ γλῶσσα μου στὶς ἀμμουδιὲς τοῦ Ὁμήρου", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_4() {
		test("ᛋᚳᛖᚪᛚ᛫ᚦᛖᚪᚻ᛫ᛗᚪᚾᚾᚪ᛫ᚷᛖᚻᚹᛦᛚᚳ᛫ᛗᛁᚳᛚᚢᚾ᛫ᚻᛦᛏ᛫ᛞᚫᛚᚪᚾ", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_5() {
		test("ღმერთსი შემვედრე, ნუთუ კვლა დამხსნას სოფლისა", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_6() {
		test("ಭವ ಭವದಿ ಭತಿಸಿಹೇ ಭವತಿ ದೂರ", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_7() {
		test("比較のとき，大文字と小文字の同一視", StandardCharsets.UTF_8);
	}
	
	@Test
	public void testUTF8_8() {
		test("$¢ह€한𐍈", StandardCharsets.UTF_8);
	}
	
	public static void test(String s, Charset charset) {
		test(s, charset, 4096);
		test(s, charset, 16);
		test(s, charset, 4);
	}
	
	public static void test(String s, Charset charset, int bufferSize) {
		CharacterDecoder decoder = CharacterDecoder.get(charset, bufferSize);
		test(s, decoder);
		if (!(decoder instanceof CharacterDecoderFromCharsetDecoder))
			test(s, new CharacterDecoderFromCharsetDecoder(charset.newDecoder(), bufferSize));
	}
	
	public static void test(String s, CharacterDecoder decoder) {
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
	}
	
	private static void check(String s, Chars.Readable chars) {
		Assert.assertEquals("Length", s.length(), chars.remaining());
		for (int i = 0; i < s.length(); ++i) {
			Assert.assertTrue(chars.hasRemaining());
			Assert.assertEquals("Char " + i, s.charAt(i), chars.get());
		}
	}
	
}
