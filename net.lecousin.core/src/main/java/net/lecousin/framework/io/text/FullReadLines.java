package net.lecousin.framework.io.text;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Task reading a text file line by line.
 * @param <T> type of object returned
 */
public abstract class FullReadLines<T> {

	/** Constructor. */
	public FullReadLines(String description, ICharacterStream.Readable.Buffered stream, byte priority, IO.OperationType closeStreamAtEnd) {
		this.description = description;
		this.priority = priority;
		this.stream = stream;
		this.closeStreamAtEnd = closeStreamAtEnd;
	}
	
	protected String description;
	protected byte priority;
	private ICharacterStream.Readable.Buffered stream;
	private IO.OperationType closeStreamAtEnd;
	
	public String getSourceDescription() {
		return stream.getDescription();
	}
	
	/** Start to read lines in a separate task. */
	public final AsyncWork<T, Exception> start() {
		AsyncWork<T, Exception> result = new AsyncWork<>();
		resume(result);
		return result;
	}
	
	private UnprotectedStringBuffer line = null;
	
	private void resume(AsyncWork<T, Exception> result) {
		stream.canStartReading().listenAsync(new Task.Cpu.FromRunnable(description, priority, () -> {
			scan(result);
		}), true);
	}
	
	private void scan(AsyncWork<T, Exception> result) {
		do {
			if (line == null) line = new UnprotectedStringBuffer();
			int c;
			try { c = stream.readAsync(); }
			catch (Exception e) {
				error(e, result);
				return;
			}
			if (c == -1) {
				if (line.length() > 0)
					try {
						processLine(line);
					} catch (Exception e) {
						error(e, result);
						return;
					}
				finish(result);
				return;
			}
			if (c == -2) {
				resume(result);
				return;
			}
			if (c == '\r') continue;
			if (c == '\n') {
				try {
					processLine(line);
				} catch (Exception e) {
					error(e, result);
					return;
				}
				line = null;
				continue;
			}
			line.append((char)c);
		} while (true);
	}
	
	private void finish(AsyncWork<T, Exception> result) {
		T r;
		try { r = generateResult(); }
		catch (Exception e) {
			error(e, result);
			return;
		}
		if (closeStreamAtEnd == null)
			result.unblockSuccess(r);
		else if (IO.OperationType.SYNCHRONOUS.equals(closeStreamAtEnd)) {
			stream.closeAsync().listenInline(() -> {
				result.unblockSuccess(r);
			});
		} else {
			result.unblockSuccess(r);
			stream.closeAsync();
		}
	}

	private void error(Exception error, AsyncWork<T, Exception> result) {
		if (closeStreamAtEnd == null)
			result.error(error);
		else if (IO.OperationType.SYNCHRONOUS.equals(closeStreamAtEnd)) {
			stream.closeAsync().listenInline(() -> {
				result.error(error);
			});
		} else {
			result.error(error);
			stream.closeAsync();
		}
	}
	
	protected abstract void processLine(UnprotectedStringBuffer line) throws Exception;
	
	protected abstract T generateResult() throws Exception;
	
}
