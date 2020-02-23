package net.lecousin.framework.io.text;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.text.CharArrayStringBuffer;

/** Task reading a text file line by line.
 * @param <T> type of object returned
 */
public abstract class FullReadLines<T> {

	/** Constructor. */
	public FullReadLines(String description, ICharacterStream.Readable.Buffered stream, Priority priority, IO.OperationType closeStreamAtEnd) {
		this.description = description;
		this.priority = priority;
		this.stream = stream;
		this.closeStreamAtEnd = closeStreamAtEnd;
	}
	
	protected String description;
	protected Priority priority;
	private ICharacterStream.Readable.Buffered stream;
	private IO.OperationType closeStreamAtEnd;
	
	public String getSourceDescription() {
		return stream.getDescription();
	}
	
	/** Start to read lines in a separate task. */
	public final AsyncSupplier<T, Exception> start() {
		AsyncSupplier<T, Exception> result = new AsyncSupplier<>();
		resume(result);
		return result;
	}
	
	private CharArrayStringBuffer line = null;
	
	private void resume(AsyncSupplier<T, Exception> result) {
		stream.canStartReading().thenStart(Task.cpu(description, priority, () -> scan(result)), true);
	}
	
	private Void scan(AsyncSupplier<T, Exception> result) {
		do {
			if (line == null) line = new CharArrayStringBuffer();
			int c;
			try { c = stream.readAsync(); }
			catch (Exception e) {
				error(e, result);
				return null;
			}
			if (c == -1) {
				if (line.length() > 0)
					try {
						processLine(line);
					} catch (Exception e) {
						error(e, result);
						return null;
					}
				finish(result);
				return null;
			}
			if (c == -2) {
				resume(result);
				return null;
			}
			if (c == '\r') continue;
			if (c == '\n') {
				try {
					processLine(line);
				} catch (Exception e) {
					error(e, result);
					return null;
				}
				line = null;
				continue;
			}
			line.append((char)c);
		} while (true);
	}
	
	private void finish(AsyncSupplier<T, Exception> result) {
		T r;
		try { r = generateResult(); }
		catch (Exception e) {
			error(e, result);
			return;
		}
		if (closeStreamAtEnd == null)
			result.unblockSuccess(r);
		else if (IO.OperationType.SYNCHRONOUS.equals(closeStreamAtEnd)) {
			stream.closeAsync().onDone(() -> result.unblockSuccess(r));
		} else {
			result.unblockSuccess(r);
			stream.closeAsync();
		}
	}

	private void error(Exception error, AsyncSupplier<T, Exception> result) {
		if (closeStreamAtEnd == null)
			result.error(error);
		else if (IO.OperationType.SYNCHRONOUS.equals(closeStreamAtEnd)) {
			stream.closeAsync().onDone(() -> result.error(error));
		} else {
			result.error(error);
			stream.closeAsync();
		}
	}

	@SuppressWarnings("squid:S00112") // implementation should be free to throw any type
	protected abstract void processLine(CharArrayStringBuffer line) throws Exception;
	
	@SuppressWarnings("squid:S00112") // implementation should be free to throw any type
	protected abstract T generateResult() throws Exception;
	
}
