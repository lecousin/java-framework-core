package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Allow to read a text file line by line. */
public class TextLineStream implements AutoCloseable, AsyncCloseable<IOException> {

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
	public void close() throws Exception {
		if (input != null) input.close();
		input = null;
	}
	
	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		return input != null ? input.closeAsync() : new SynchronizationPoint<>(true);
	}
	
}
