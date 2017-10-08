package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;

/** Buffered readable character stream that calculate the line number and position on the current line while reading. */
public class BufferedReadableCharacterStreamLocation implements ICharacterStream.Readable.Buffered {

	/** Constructor. */
	public BufferedReadableCharacterStreamLocation(BufferedReadableCharacterStream stream) {
		this.stream = stream;
	}
	
	private BufferedReadableCharacterStream stream;
	private int line = 1;
	private int pos = 0;
	private int lastLinePos = 0;
	
	public int getLine() { return line; }
	
	public int getPositionInLine() { return pos; }
	
	@Override
	public String getSourceDescription() { return stream.getSourceDescription(); }
	
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
	public int read(char[] buf, int offset, int length) {
		int nb = stream.read(buf, offset, length);
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
	public void close() {
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
