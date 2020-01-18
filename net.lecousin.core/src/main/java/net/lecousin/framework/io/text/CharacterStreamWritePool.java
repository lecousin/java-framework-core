package net.lecousin.framework.io.text;

import java.io.IOException;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.data.RawCharBuffer;
import net.lecousin.framework.text.IString;

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
	private IAsync<IOException> lastWrite = new Async<>(true);
	
	/** Write a single character. */
	public IAsync<IOException> write(char c) {
		if (!(stream instanceof ICharacterStream.Writable.Buffered))
			return write(new char[] { c }, 0, 1);
		IAsync<IOException> last = lastWrite;
		if (last.isDone()) {
			lastWrite = ((ICharacterStream.Writable.Buffered)stream).writeAsync(c);
			return lastWrite;
		}
		Async<IOException> ours = new Async<>();
		lastWrite = ours;
		last.onDone(() -> ((ICharacterStream.Writable.Buffered)stream).writeAsync(c).onDone(ours), ours);
		return ours;
	}
	
	/** Write the given characters. */
	public IAsync<IOException> write(char[] chars) {
		return write(chars, 0, chars.length);
	}
	
	/** Write the given characters. */
	public IAsync<IOException> write(char[] chars, int offset, int length) {
		IAsync<IOException> last = lastWrite;
		if (length == 0) return last;
		if (last.isDone()) {
			lastWrite = stream.writeAsync(chars, offset, length);
			return lastWrite;
		}
		Async<IOException> ours = new Async<>();
		lastWrite = ours;
		last.onDone(() -> stream.writeAsync(chars, offset, length).onDone(ours), ours);
		return ours;
	}
	
	/** Write the given characters. */
	public IAsync<IOException> write(RawCharBuffer chars) {
		return write(chars.array, chars.currentOffset, chars.remaining());
	}
	
	/** Write the given characters. */
	public IAsync<IOException> write(RawCharBuffer[] chars) {
		IAsync<IOException> last = lastWrite;
		for (int i = 0; i < chars.length; ++i)
			last = write(chars[i]);
		return last;
	}
	
	/** Write the given string. */
	public IAsync<IOException> write(String s) {
		return write(s.toCharArray());
	}
	
	/** Write the given string. */
	public IAsync<IOException> write(IString s) {
		return write(s.asCharBuffers());
	}

	/** Write the given string. */
	public IAsync<IOException> write(CharSequence s) {
		return write(s.toString());
	}

	/** Returns the synchronization point of the latest write operation. */
	public IAsync<IOException> flush() {
		return lastWrite;
	}
	
}
