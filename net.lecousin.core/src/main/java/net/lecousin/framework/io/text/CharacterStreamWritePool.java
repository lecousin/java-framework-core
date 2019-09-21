package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.CharBuffer;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.IString;

/** Utility class to queue write operations to a character stream.
 * Each time a new write operation is called, it will wait for the previous operation to end before to execute it.
 * This allows to <i>queue</i> write operations without blocking.
 * However it is recommended to do not queue too much operations as it will consume memory.
 */
public class CharacterStreamWritePool {

	/** Constructor. */
	public CharacterStreamWritePool(ICharacterStream.Writable stream) {
		this.stream = stream;
	}
	
	private ICharacterStream.Writable stream;
	private ISynchronizationPoint<IOException> lastWrite = new SynchronizationPoint<>(true);
	
	/** Write a single character. */
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
		last.listenInline(() -> ((ICharacterStream.Writable.Buffered)stream).writeAsync(c).listenInline(ours), ours);
		return ours;
	}
	
	/** Write the given characters. */
	public ISynchronizationPoint<IOException> write(char[] chars) {
		return write(chars, 0, chars.length);
	}
	
	/** Write the given characters. */
	public ISynchronizationPoint<IOException> write(char[] chars, int offset, int length) {
		ISynchronizationPoint<IOException> last = lastWrite;
		if (length == 0) return last;
		if (last.isUnblocked()) {
			lastWrite = stream.writeAsync(chars, offset, length);
			return lastWrite;
		}
		SynchronizationPoint<IOException> ours = new SynchronizationPoint<>();
		lastWrite = ours;
		last.listenInline(() -> stream.writeAsync(chars, offset, length).listenInline(ours), ours);
		return ours;
	}
	
	/** Write the given characters. */
	public ISynchronizationPoint<IOException> write(CharBuffer chars) {
		return write(chars.array(), chars.position(), chars.remaining());
	}
	
	/** Write the given characters. */
	public ISynchronizationPoint<IOException> write(CharBuffer[] chars) {
		ISynchronizationPoint<IOException> last = lastWrite;
		for (int i = 0; i < chars.length; ++i)
			last = write(chars[i]);
		return last;
	}
	
	/** Write the given string. */
	public ISynchronizationPoint<IOException> write(String s) {
		return write(s.toCharArray());
	}
	
	/** Write the given string. */
	public ISynchronizationPoint<IOException> write(IString s) {
		return write(s.asCharBuffers());
	}

	/** Write the given string. */
	public ISynchronizationPoint<IOException> write(CharSequence s) {
		return write(s.toString());
	}

	/** Returns the synchronization point of the latest write operation. */
	public ISynchronizationPoint<IOException> flush() {
		return lastWrite;
	}
	
}
