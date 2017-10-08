package net.lecousin.framework.io.encoding;

/**
 * Handle a number encoding: characters can be added, then the current decoded value can be retrieved.
 */
public interface INumberEncoding {

	/** A new character to decode. */
	public boolean addChar(char c);
	
	/** Get the current decoded value. */
	public long getNumber();
	
	/** Reset. */
	public void reset();
	
}
