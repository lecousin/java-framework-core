package net.lecousin.framework.io.text;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.text.CharArrayStringBuffer;

/** Implementation of FullReadLines to read a file containing properties.
 * @param <T> type of object returned as result
 */
public abstract class PropertiesReader<T> extends FullReadLines<T> {

	/** Constructor. */
	public PropertiesReader(
		char commentChar, String description,
		ICharacterStream.Readable.Buffered stream,
		Priority priority, IO.OperationType closeStreamAtEnd
	) {
		super(description, stream, priority, closeStreamAtEnd);
		this.commentChar = commentChar;
	}
	
	/** Constructor with default comment character #. */
	public PropertiesReader(String description, ICharacterStream.Readable.Buffered stream, Priority priority, IO.OperationType closeStreamAtEnd) {
		this('#', description, stream, priority, closeStreamAtEnd);
	}
	
	private char commentChar;
	
	@Override
	protected void processLine(CharArrayStringBuffer line) {
		if (line.isEmpty()) return;
		if (line.charAt(0) == commentChar) return;
		int i = line.indexOf('=');
		if (i < 0) return;
		CharArrayStringBuffer key = line.substring(0,i);
		key.trim();
		CharArrayStringBuffer value = line.substring(i + 1);
		value.trim();
		processProperty(key, value);
	}
	
	protected abstract void processProperty(CharArrayStringBuffer key, CharArrayStringBuffer value);

}
