package net.lecousin.framework.encoding.charset;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.data.CharsFromIso8859Bytes;

/** US-ASCII decoder. */
public class UsAsciiDecoder implements CharacterDecoder {

	/** Constructor. */
	public UsAsciiDecoder(@SuppressWarnings({"unused","squid:S1172"}) int bufferSize) {
		this();
	}
	
	/** Constructor. */
	public UsAsciiDecoder() {
	}
	
	@Override
	public Chars.Readable decode(Bytes.Readable input) {
		return new CharsFromIso8859Bytes(input, true);
	}

	@Override
	public Chars.Readable flush() {
		return null;
	}
	
	@Override
	public Charset getEncoding() {
		return StandardCharsets.US_ASCII;
	}
}
