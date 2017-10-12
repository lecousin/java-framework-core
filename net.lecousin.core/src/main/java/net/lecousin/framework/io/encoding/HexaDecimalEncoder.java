package net.lecousin.framework.io.encoding;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.util.StringUtil;

/** Encode and decode bytes using hexadecimal digits. */
public class HexaDecimalEncoder implements IBytesEncoding {

	@Override
	public byte[] encode(byte[] bytes) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < bytes.length; ++i)
			s.append(StringUtil.encodeHexaDigit((bytes[i] & 0xF0) >> 4)).append(StringUtil.encodeHexaDigit(bytes[i] & 0xF));
		return s.toString().getBytes(StandardCharsets.ISO_8859_1);
	}
	
	@Override
	public byte[] decode(byte[] encoded) {
		byte[] s = new byte[encoded.length / 2];
		for (int i = 0; i < s.length; ++i)
			s[i] = (byte)((StringUtil.decodeHexa((char)encoded[i * 2]) << 4) | StringUtil.decodeHexa((char)encoded[i * 2 + 1]));
		return s;
	}
	
}
