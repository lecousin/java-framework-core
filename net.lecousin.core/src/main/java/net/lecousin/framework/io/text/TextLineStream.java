package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Allow to read a text file line by line. */
public class TextLineStream extends ConcurrentCloseable {

	/** Constructor. */
	public TextLineStream(ICharacterStream.Readable.Buffered input) {
		this.input = input;
	}
	
	private ICharacterStream.Readable.Buffered input;
	
	/** Read a new line, return null if the end of the character stream is reached. */
	public UnprotectedStringBuffer nextLine() throws IOException {
		if (input.endReached()) return null;
		UnprotectedStringBuffer line = new UnprotectedStringBuffer();
		do {
			try {
				char c = input.read();
				if (c == '\n') break;
				line.append(c);
			} catch (EOFException e) {
				break;
			}
		} while (true);
		return line;
	}
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return input.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		input = null;
		ondone.unblock();
	}

	@Override
	public byte getPriority() {
		return input.getPriority();
	}
}
