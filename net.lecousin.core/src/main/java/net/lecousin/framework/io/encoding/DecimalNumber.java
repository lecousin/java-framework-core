package net.lecousin.framework.io.encoding;

/**
 * Decode a number in decimal digits.
 */
public class DecimalNumber implements INumberEncoding {

	private long value = 0;
	
	@Override
	public boolean addChar(char c) {
		if (c < '0') return false;
		if (c > '9') return false;
		value *= 10;
		value += (c - '0');
		return true;
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
