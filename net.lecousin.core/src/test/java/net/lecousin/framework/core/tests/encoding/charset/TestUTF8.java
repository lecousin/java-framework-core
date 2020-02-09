package net.lecousin.framework.core.tests.encoding.charset;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.encoding.charset.AbstractTestCharsetDecoder;
import net.lecousin.framework.encoding.charset.UTF8Decoder;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.text.CharArrayString;

import org.junit.Assert;
import org.junit.Test;

public class TestUTF8 extends AbstractTestCharsetDecoder {

	public TestUTF8() {
		super(StandardCharsets.UTF_8);
	}
	
	@Override
	protected String[] getTestStrings() {
		return new String[] {
			"abcd012GH)I_V;",
			"И вдаль глядел. Пред ним широко",
			"Μονάχη ἔγνοια ἡ γλῶσσα μου στὶς ἀμμουδιὲς τοῦ Ὁμήρου",
			"ᛋᚳᛖᚪᛚ᛫ᚦᛖᚪᚻ᛫ᛗᚪᚾᚾᚪ᛫ᚷᛖᚻᚹᛦᛚᚳ᛫ᛗᛁᚳᛚᚢᚾ᛫ᚻᛦᛏ᛫ᛞᚫᛚᚪᚾ",
			"ღმერთსი შემვედრე, ნუთუ კვლა დამხსნას სოფლისა",
			"ಭವ ಭವದಿ ಭತಿಸಿಹೇ ಭವತಿ ದೂರ",
			"比較のとき，大文字と小文字の同一視",
			"$¢ह€한𐍈"
		};
	}
	
	@Test
	public void testInvalidChars() {
		testInvalidChar(new byte[] { (byte)0xC7, (byte)0xC8 });
		testInvalidChar(new byte[] { (byte)0xE3, (byte)0x86, (byte)0xC2 });
		testInvalidChar(new byte[] { (byte)0xE3, (byte)0xC6, (byte)0xC2 });
		testInvalidChar(new byte[] { (byte)0xF1, (byte)0xC6, (byte)0xC5, (byte)0xC4 });
		testInvalidChar(new byte[] { (byte)0xF1, (byte)0x86, (byte)0xC5, (byte)0xC4 });
		testInvalidChar(new byte[] { (byte)0xF1, (byte)0x86, (byte)0x85, (byte)0xC4 });
		testInvalidChar(new byte[] { (byte)0xFF });
	}
	
	private static void testInvalidChar(byte[] bytes) {
		byte[] b = new byte[6 + bytes.length];
		b[0] = 'a';
		b[1] = 'b';
		System.arraycopy(bytes, 0, b, 2, bytes.length);
		b[bytes.length + 2] = 'z';
		b[bytes.length + 3] = 'y';
		b[bytes.length + 4] = '0';
		b[bytes.length + 5] = '1';
		
		testInvalidChar(b, "ab" + UTF8Decoder.INVALID_CHAR + "zy01");
		byte[] b2 = new byte[b.length - 4];
		System.arraycopy(b, 0, b2, 0, b2.length);
		testInvalidChar(b2, "ab" + UTF8Decoder.INVALID_CHAR);
		b2 = new byte[b.length - 5];
		System.arraycopy(b, 1, b2, 0, b2.length);
		testInvalidChar(b2, "b" + UTF8Decoder.INVALID_CHAR);
		b2 = new byte[b.length - 6];
		System.arraycopy(b, 2, b2, 0, b2.length);
		testInvalidChar(b2, "" + UTF8Decoder.INVALID_CHAR);
	}
	
	private static void testInvalidChar(byte[] b, String expected) {
		testInvalidChar(b, expected, 64);
		testInvalidChar(b, expected, 16);
		testInvalidChar(b, expected, 8);
		testInvalidChar(b, expected, 4);
		testInvalidChar(b, expected, 2);
	}
	
	private static void testInvalidChar(byte[] b, String expected, int bufferSize) {
		UTF8Decoder decoder = new UTF8Decoder(bufferSize);
		RawByteBuffer input = new RawByteBuffer(b);
		CharArrayString output = new CharArrayString(256);
		while (input.hasRemaining()) {
			Chars.Readable chars = decoder.decode(input);
			chars.get(output, chars.remaining());
		}
		Assert.assertEquals(expected, output.asString());
	}
	
}
