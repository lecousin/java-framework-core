package net.lecousin.framework.io.text;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Implementation of FullReadLines to read a file containing properties.
 * @param <T> type of object returned as result
 */
public abstract class PropertiesReader<T> extends FullReadLines<T> {

	/** Constructor. */
	public PropertiesReader(
		char commentChar, String description,
		ICharacterStream.Readable.Buffered stream,
		byte priority, IO.OperationType closeStreamAtEnd
	) {
		super(description, stream, priority, closeStreamAtEnd);
		this.commentChar = commentChar;
	}
	
	/** Constructor with default comment character #. */
	public PropertiesReader(String description, ICharacterStream.Readable.Buffered stream, byte priority, IO.OperationType closeStreamAtEnd) {
		this('#', description, stream, priority, closeStreamAtEnd);
	}
	
	private char commentChar;
	
	@Override
	protected void processLine(UnprotectedStringBuffer line) {
		if (line.length() == 0) return;
		if (line.charAt(0) == commentChar) return;
		int i = line.indexOf('=');
		if (i < 0) return;
		UnprotectedStringBuffer key = line.substring(0,i);
		key.trim();
		UnprotectedStringBuffer value = line.substring(i + 1);
		value.trim();
		processProperty(key, value);
	}
	
	protected abstract void processProperty(UnprotectedStringBuffer key, UnprotectedStringBuffer value);

}
