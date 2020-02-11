package net.lecousin.framework.core.tests.encoding.charset;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.encoding.charset.AbstractTestCharsetDecoder;

public class TestUTF16BE extends AbstractTestCharsetDecoder {

	public TestUTF16BE() {
		super(StandardCharsets.UTF_16BE);
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
			"$¢ह€한𐍈",
			"世界有很多语言𤭢"
		};
	}
	
}
