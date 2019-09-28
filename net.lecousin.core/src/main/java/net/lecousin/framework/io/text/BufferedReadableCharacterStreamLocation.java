package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.UnprotectedString;

/** Buffered readable character stream that calculate the line number and position on the current line while reading. */
public class BufferedReadableCharacterStreamLocation extends ConcurrentCloseable<IOException> implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public BufferedReadableCharacterStreamLocation(ICharacterStream.Readable.Buffered stream) {
		this(stream, 1, 0);
	}

	/** Constructor. */
	public BufferedReadableCharacterStreamLocation(ICharacterStream.Readable.Buffered stream, int line, int posInLine) {
		this.stream = stream;
		this.line = line;
		this.lastLinePos = this.pos = posInLine;
	}
	
	private ICharacterStream.Readable.Buffered stream;
	private int line;
	private int pos;
	private int lastLinePos;
	
	public int getLine() { return line; }
	
	public int getPositionInLine() { return pos; }
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return stream.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		stream = null;
		ondone.unblock();
	}
	
	@Override
	public String getDescription() { return stream.getDescription(); }
	
	@Override
	public Charset getEncoding() {
		return stream.getEncoding();
	}
	
	@Override
	public byte getPriority() {
		return stream.getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		stream.setPriority(priority);
	}
	
	@Override
	public char read() throws IOException {
		char c;
		if ((c = stream.read()) == '\n') {
			line++;
			lastLinePos = pos;
			pos = 0;
		} else if (c != '\r') {
			pos++;
		}
		return c;
	}

	@Override
	public int readSync(char[] buf, int offset, int length) throws IOException {
		int nb = stream.readSync(buf, offset, length);
		for (int i = 0; i < nb; ++i)
			if (buf[offset + i] == '\n') {
				line++;
				lastLinePos = pos;
				pos = 0;
			} else if (buf[offset + i] != '\r') {
				pos++;
			}
		return nb;
	}
	
	@Override
	public void back(char c) {
		if (c == '\n') {
			line--;
			pos = lastLinePos;
		} else {
			pos--;
		}
		stream.back(c);
	}
	
	@Override
	public int readAsync() throws IOException {
		int c = stream.readAsync();
		if (c < 0) return c;
		if (c == '\n') {
			line++;
			lastLinePos = pos;
			pos = 0;
		} else if (c != '\r') {
			pos++;
		}
		return c;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(char[] buf, int offset, int length) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		AsyncSupplier<Integer, IOException> read = stream.readAsync(buf, offset, length);
		read.thenStart(operation(new Task.Cpu<Void, NoException>(
			"Calculate new location of BufferedReadableCharacterStreamLocation", stream.getPriority()
		) {
			@Override
			public Void run() {
				if (read.hasError())
					result.error(read.getError());
				else if (read.isCancelled())
					result.cancel(read.getCancelEvent());
				else {
					int nb = read.getResult().intValue();
					for (int i = 0; i < nb; ++i)
						if (buf[offset + i] == '\n') {
							line++;
							lastLinePos = pos;
							pos = 0;
						} else if (buf[offset + i] != '\r') {
							pos++;
						}
					result.unblockSuccess(read.getResult());
				}
				return null;
			}
		}), true);
		return result;
	}
	
	@Override
	public AsyncSupplier<UnprotectedString, IOException> readNextBufferAsync() {
		AsyncSupplier<UnprotectedString, IOException> result = new AsyncSupplier<>();
		AsyncSupplier<UnprotectedString, IOException> read = stream.readNextBufferAsync();
		read.thenStart(
		new Task.Cpu.FromRunnable("Calculate new location of BufferedReadableCharacterStreamLocation", stream.getPriority(), () -> {
			UnprotectedString str = read.getResult();
			if (str == null) {
				result.unblockSuccess(null);
				return;
			}
			char[] buf = str.charArray();
			int offset = str.charArrayStart();
			int len = str.length();
			for (int i = 0; i < len; ++i)
				if (buf[offset + i] == '\n') {
					line++;
					lastLinePos = pos;
					pos = 0;
				} else if (buf[offset + i] != '\r') {
					pos++;
				}
			result.unblockSuccess(str);
		}), result);
		return result;
	}
	
	@Override
	public boolean endReached() {
		return stream.endReached();
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return stream.canStartReading();
	}
}
