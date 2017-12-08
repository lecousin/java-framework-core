package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.CharBuffer;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.IString;

public class CharacterStreamWritePool {

	public CharacterStreamWritePool(ICharacterStream.Writable stream) {
		this.stream = stream;
	}
	
	private ICharacterStream.Writable stream;
	private ISynchronizationPoint<IOException> lastWrite = new SynchronizationPoint<>(true);
	
	public ISynchronizationPoint<IOException> write(char c) {
		if (!(stream instanceof ICharacterStream.Writable.Buffered))
			return write(new char[] { c }, 0, 1);
		ISynchronizationPoint<IOException> last = lastWrite;
		if (last.isUnblocked()) {
			lastWrite = ((ICharacterStream.Writable.Buffered)stream).writeAsync(c);
			return lastWrite;
		}
		SynchronizationPoint<IOException> ours = new SynchronizationPoint<>();
		lastWrite = ours;
		last.listenInline(() -> { ((ICharacterStream.Writable.Buffered)stream).writeAsync(c).listenInline(ours); }, ours);
		return ours;
	}
	
	public ISynchronizationPoint<IOException> write(char[] chars) {
		return write(chars, 0, chars.length);
	}
	
	public ISynchronizationPoint<IOException> write(char[] chars, int offset, int length) {
		ISynchronizationPoint<IOException> last = lastWrite;
		if (length == 0) return last;
		if (last.isUnblocked()) {
			lastWrite = stream.writeAsync(chars, offset, length);
			return lastWrite;
		}
		SynchronizationPoint<IOException> ours = new SynchronizationPoint<>();
		lastWrite = ours;
		last.listenInline(() -> { stream.writeAsync(chars, offset, length).listenInline(ours); }, ours);
		return ours;
	}
	
	public ISynchronizationPoint<IOException> write(CharBuffer chars) {
		return write(chars.array(), chars.position(), chars.remaining());
	}
	
	public ISynchronizationPoint<IOException> write(CharBuffer[] chars) {
		ISynchronizationPoint<IOException> last = lastWrite;
		for (int i = 0; i < chars.length; ++i)
			last = write(chars[i]);
		return last;
	}
	
	public ISynchronizationPoint<IOException> write(String s) {
		return write(s.toCharArray());
	}
	
	
	public ISynchronizationPoint<IOException> write(IString s) {
		return write(s.asCharBuffers());
	}

	
	public ISynchronizationPoint<IOException> write(CharSequence s) {
		return write(s.toString());
	}
	
	public ISynchronizationPoint<IOException> flush() {
		return lastWrite;
	}
	
}
