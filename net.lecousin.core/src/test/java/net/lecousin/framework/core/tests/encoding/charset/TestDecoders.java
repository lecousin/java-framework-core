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
		Chars.Readable chars = decoder.decode(new RawByteBuffer(s.getBytes(decoder.getEncoding())));
		Chars.Readable end = decoder.flush();
		if (end != null)
			chars = new CompositeChars.Readable(chars, end);
		Assert.assertEquals(s.length(), chars.remaining());
		for (int i = 0; i < s.length(); ++i) {
			Assert.assertTrue(chars.hasRemaining());
			Assert.assertEquals(s.charAt(i), chars.get());
		}
	}
	
}
