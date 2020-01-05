package net.lecousin.framework.encoding.charset;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.BytesAsIso8859Chars;
import net.lecousin.framework.io.data.Chars;

/** ISO-8859-1 decoder. */
public class Iso8859Decoder implements CharacterDecoder {

	/** Constructor. */
	public Iso8859Decoder(@SuppressWarnings({"unused","squid:S1172"}) int bufferSize) {
		this();
	}
	
	/** Constructor. */
	public Iso8859Decoder() {
	}
	
	@Override
	public Chars.Readable decode(Bytes.Readable input) {
		return new BytesAsIso8859Chars(input);
	}

	@Override
	public Chars.Readable flush() {
		return null;
	}
	
	@Override
	public Charset getEncoding() {
		return StandardCharsets.ISO_8859_1;
	}
}
