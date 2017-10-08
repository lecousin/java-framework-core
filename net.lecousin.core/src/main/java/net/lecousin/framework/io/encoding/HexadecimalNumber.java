package net.lecousin.framework.io.encoding;

/**
 * Decode a number in hexadecimal digits.
 */
public class HexadecimalNumber implements INumberEncoding {

	private long value = 0;
	
	@Override
	public boolean addChar(char c) {
		if (c < '0') return false;
		if (c <= '9') {
			value *= 16;
			value += (c - '0');
			return true;
		}
		if (c < 'A') return false;
		if (c <= 'F') {
			value *= 16;
			value += (c - 'A' + 10);
			return true;
		}
		if (c < 'a') return false;
		if (c <= 'f') {
			value *= 16;
			value += (c - 'a' + 10);
			return true;
		}
		return false;
	}
	
	@Override
	public long getNumber() {
		return value;
	}
	
	@Override
	public void reset() {
		value = 0;
	}
	
}
