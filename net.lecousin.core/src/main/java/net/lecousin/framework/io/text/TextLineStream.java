package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.text.CharArrayStringBuffer;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Allow to read a text file line by line. */
public class TextLineStream extends ConcurrentCloseable<IOException> {

	/** Constructor. */
	public TextLineStream(ICharacterStream.Readable.Buffered input) {
		this.input = input;
	}
	
	private ICharacterStream.Readable.Buffered input;
	
	/** Read a new line, return null if the end of the character stream is reached. */
	public CharArrayStringBuffer nextLine() throws IOException {
		if (input.endReached()) return null;
		CharArrayStringBuffer line = new CharArrayStringBuffer();
		boolean prevCR = false;
		do {
			try {
				char c = input.read();
				if (c == '\n') break;
				if (c == '\r' && !prevCR) {
					prevCR = true;
					continue;
				}
				if (prevCR) {
					line.append('\r');
					prevCR = false;
				}
				if (c == '\r') {
					prevCR = true;
					continue;
				}
				line.append(c);
			} catch (EOFException e) {
				break;
			}
		} while (true);
		return line;
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return input.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		input = null;
		ondone.unblock();
	}

	@Override
	public byte getPriority() {
		return input.getPriority();
	}
}
