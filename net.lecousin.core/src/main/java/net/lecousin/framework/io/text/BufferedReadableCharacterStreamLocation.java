package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.exception.NoException;

/** Buffered readable character stream that calculate the line number and position on the current line while reading. */
public class BufferedReadableCharacterStreamLocation implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public BufferedReadableCharacterStreamLocation(ICharacterStream.Readable.Buffered stream) {
		this.stream = stream;
	}
	
	private ICharacterStream.Readable.Buffered stream;
	private int line = 1;
	private int pos = 0;
	private int lastLinePos = 0;
	
	public int getLine() { return line; }
	
	public int getPositionInLine() { return pos; }
	
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
	public char read() throws EOFException, IOException {
		char c = stream.read();
		if (c == '\n') {
			line++;
			lastLinePos = pos;
			pos = 0;
		} else if (c != '\r')
			pos++;
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
			} else if (buf[offset + i] != '\r')
				pos++;
		return nb;
	}
	
	@Override
	public void back(char c) {
		if (c == '\n') {
			line--;
			pos = lastLinePos;
		} else
			pos--;
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
		} else if (c != '\r')
			pos++;
		return c;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(char[] buf, int offset, int length) {
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		AsyncWork<Integer, IOException> read = stream.readAsync(buf, offset, length);
		read.listenAsynch(new Task.Cpu<Void, NoException>(
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
						} else if (buf[offset + i] != '\r')
							pos++;
					result.unblockSuccess(read.getResult());
				}
				return null;
			}
		}, true);
		return result;
	}
	
	@Override
	public void close() throws Exception {
		stream.close();
	}
	
	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		return stream.closeAsync();
	}
	
	@Override
	public boolean endReached() {
		return stream.endReached();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return stream.canStartReading();
	}
}
