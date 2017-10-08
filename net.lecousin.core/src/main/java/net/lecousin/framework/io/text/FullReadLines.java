package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Task reading a text file line by line.
 * @param <T> type of object returned
 */
public abstract class FullReadLines<T> extends Task.Cpu<T,IOException> {

	/** Constructor. */
	public FullReadLines(String description, ICharacterStream.Readable.Buffered stream, byte priority, IO.OperationType closeStreamAtEnd) {
		super(description, priority);
		this.stream = stream;
		this.closeStreamAtEnd = closeStreamAtEnd;
	}
	
	private ICharacterStream.Readable.Buffered stream;
	private IO.OperationType closeStreamAtEnd;
	
	public String getSourceDescription() {
		return stream.getSourceDescription();
	}
	
	@Override
	public final T run() throws IOException {
		try {
			do {
				UnprotectedStringBuffer line = new UnprotectedStringBuffer();
				try {
					do {
						char c = stream.read();
						if (c == '\r') continue;
						if (c == '\n') break;
						line.append(c);
					} while (true);
				} catch (EOFException e) {
					if (line.length() > 0)
						processLine(line);
					break;
				}
				processLine(line);
			} while (true);
			return generateResult();
		} finally {
			if (closeStreamAtEnd != null) {
				if (IO.OperationType.SYNCHRONOUS.equals(closeStreamAtEnd))
					try { stream.close(); } catch (Exception e) { /* ignore */ }
				else
					stream.closeAsync();
			}
		}
	}
	
	protected abstract void processLine(UnprotectedStringBuffer line) throws IOException;
	
	protected abstract T generateResult() throws IOException;
	
}
