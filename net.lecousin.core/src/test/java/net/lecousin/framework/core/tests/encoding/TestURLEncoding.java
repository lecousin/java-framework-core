package net.lecousin.framework.core.tests.encoding;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.encoding.URLEncoding;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CharsFromString;
import net.lecousin.framework.text.ByteArrayStringIso8859Buffer;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestURLEncoding extends LCCoreAbstractTest {
	
	private static String[] strings = new String[] {
		"hello world",
		"hello (*!?&) world",
		"hello глядел",
		"Μονάχη ἔγνοια ἡ γλῶσσα μου στὶς ἀμμουδιὲς τοῦ Ὁμήρου",
		"ᛋᚳᛖᚪᛚ᛫ᚦᛖᚪᚻ᛫ᛗᚪᚾᚾᚪ᛫ᚷᛖᚻᚹᛦᛚᚳ᛫ᛗᛁᚳᛚᚢᚾ᛫ᚻᛦᛏ᛫ᛞᚫᛚᚪᚾ",
		"ღმერთსი შემვედრე, ნუთუ კვლა დამხსნას სოფლისა",
		"ಭವ ಭವದಿ ಭತಿಸಿಹೇ ಭವತಿ ದೂರ",
		"比較のとき，大文字と小文字の同一視",
		"$¢ह€한𐍈",
	};
	
	@Parameters(name = "nbBuf = {1}")
	public static Collection<Object[]> parameters() {
		List<Object[]> list = new LinkedList<>();
		for (String s : strings) {
			list.add(new Object[] { s, Boolean.FALSE });
			list.add(new Object[] { s + "/" + s, Boolean.FALSE });
			list.add(new Object[] { s + "/" + s, Boolean.TRUE });
		}
		return list;
	}
	
	public TestURLEncoding(String str, boolean isPath) throws Exception {
		this.str = str;
		this.isPath = isPath;
		if (isPath) {
			String[] strs = str.split("\\/");
			StringBuilder s = new StringBuilder();
			for (String ss : strs) {
				if (s.length() > 0) s.append('/');
				s.append(URLEncoder.encode(ss, "UTF-8"));
			}
			expected = s.toString();
		} else {
			expected = URLEncoder.encode(str, "UTF-8");
		}
	}
	
	private String str;
	private boolean isPath;
	private String expected; 

	@Test
	public void testEncodingFull() throws Exception {
		ByteArrayStringIso8859Buffer bytes = new ByteArrayStringIso8859Buffer();
		URLEncoding.encode(new CharsFromString(str), bytes.asWritableBytes(), isPath, true);
		String encoded = bytes.asString();
		Assert.assertEquals(expected, encoded);
	}
	
	@Test
	public void testEncodingByteByByte() throws Exception {
		byte[] bytes = new byte[8192];
		CharsFromString chars = new CharsFromString(str);
		int pos = 0;
		int len = 1;
		do {
			ByteArray.Writable b = new ByteArray.Writable(bytes, pos, len, false);
			URLEncoding.encode(chars, b, isPath, true);
			if (b.getCurrentArrayOffset() == pos)
				len++;
			else {
				pos = b.getCurrentArrayOffset();
				len = 1;
			}
		} while (chars.hasRemaining());
		Assert.assertEquals(expected, new String(bytes, 0, pos, StandardCharsets.US_ASCII));
	}
	
	@Test
	public void testEncodingCharByChar() throws Exception {
		ByteArrayStringIso8859Buffer bytes = new ByteArrayStringIso8859Buffer();
		Bytes.Writable out = bytes.asWritableBytes();
		CharsFromString chars = new CharsFromString(str);
		int len = 1;
		do {
			Chars.Readable in = chars.subBuffer(chars.position(), len);
			URLEncoding.encode(in, out, isPath, chars.remaining() == len);
			if (in.position() == 0) {
				len++;
			} else {
				chars.moveForward(in.position());
				len = 1;
			}
		} while (chars.hasRemaining());
		Assert.assertEquals(expected, bytes.asString());
	}
	
	@Test
	public void testDecodeFull() throws Exception {
		CharArrayStringBuffer decoded = URLEncoding.decode(new ByteArray(expected.getBytes(StandardCharsets.US_ASCII)));
		Assert.assertEquals(str, decoded.asString());
	}
	
	@Test
	public void testDecodeCharByChar() throws Exception {
		char[] chars = new char[8192];
		ByteArray bytes = new ByteArray(expected.getBytes(StandardCharsets.US_ASCII));
		int pos = 0;
		int len = 1;
		do {
			CharArray.Writable b = new CharArray.Writable(chars, pos, len, false);
			URLEncoding.decode(bytes, b, true);
			if (b.getCurrentArrayOffset() == pos)
				len++;
			else {
				pos = b.getCurrentArrayOffset();
				len = 1;
			}
		} while (bytes.hasRemaining());
		Assert.assertEquals(str, new String(chars, 0, pos));
	}
	
	@Test
	public void testDecodeByteByByte() throws Exception {
		CharArray.Writable out = new CharArray.Writable(new char[8192], false);
		ByteArray bytes = new ByteArray(expected.getBytes(StandardCharsets.US_ASCII));
		int len = 1;
		do {
			ByteArray in = bytes.subBuffer(bytes.position(), len);
			URLEncoding.decode(in, out, bytes.remaining() == len);
			if (in.position() == 0) {
				len++;
			} else {
				bytes.moveForward(in.position());
				len = 1;
			}
		} while (bytes.hasRemaining());
		Assert.assertEquals(str, new String (out.getArray(), 0, out.position()));
	}
	
	@Test
	public void testDecodeErrors() throws Exception {
		URLEncoding.decode(new ByteArray("%G".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%GF".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%FG".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80x".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80xxxx".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%G".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%GF".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%FG".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%C0%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00x00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00xx".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00xxxx".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00%G0".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%80%00%0G".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%E0%00%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%E0%80%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00x".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00xx".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00xxx".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00%".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00%0".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00%G0".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00%0G".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F8%00%00%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%00%00%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%80%00%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%80%80%00".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F0%80%80%80".getBytes(StandardCharsets.US_ASCII)));
		URLEncoding.decode(new ByteArray("%F7%80%80%80".getBytes(StandardCharsets.US_ASCII)));
	}
	
}
