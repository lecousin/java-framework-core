package net.lecousin.framework.text;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.ConcurrentCloseable;

/**
 * Array of CharArrayString, allowing to add and remove characters without re-allocating a character array.
 * 
 * @see CharArrayString
 */
public class CharArrayStringBuffer extends ArrayStringBuffer<CharArrayString, CharArrayStringBuffer> {

	/** Create a new empty string. */
	public CharArrayStringBuffer() {
		super();
	}

	/** Create a string with the given one. */
	public CharArrayStringBuffer(CharArrayString string) {
		super(string);
	}
	
	/** Create a string with the given ones. */
	public CharArrayStringBuffer(Collection<CharArrayString> strings) {
		super(strings);
	}
	
	/** Create a string with the given one. */
	public CharArrayStringBuffer(String s) {
		strings = new CharArrayString[1];
		lastUsed = 0;
		strings[0] = new CharArrayString(s);
	}

	/** Create a string with the given one. */
	public CharArrayStringBuffer(CharSequence s) {
		strings = new CharArrayString[1];
		lastUsed = 0;
		strings[0] = new CharArrayString(s);
	}
	
	/** Create a string with the given one. */
	public CharArrayStringBuffer(IString s) {
		strings = new CharArrayString[1];
		lastUsed = 0;
		strings[0] = new CharArrayString(s);
	}
	
	@Override
	protected CharArrayString[] allocateArray(int arraySize) {
		return new CharArrayString[arraySize];
	}
	
	@Override
	protected CharArrayString createString(int initialCapacity) {
		return new CharArrayString(initialCapacity);
	}
	
	@Override
	protected CharArrayString createString(CharSequence s) {
		return new CharArrayString(s);
	}
	
	@Override
	protected CharArrayString createString(CharSequence s, int startPos, int endPos) {
		return new CharArrayString(s, startPos, endPos);
	}
	
	@Override
	protected CharArrayString createString(char singleChar) {
		return new CharArrayString(singleChar);
	}
	
	@Override
	protected CharArrayString createString(char[] chars) {
		return new CharArrayString(chars);
	}
	
	@Override
	protected CharArrayStringBuffer createBuffer() {
		return new CharArrayStringBuffer();
	}
	
	@Override
	protected CharArrayStringBuffer createBuffer(CharArrayString s) {
		return new CharArrayStringBuffer(s);
	}
	
	@Override
	protected CharArrayStringBuffer createBuffer(List<CharArrayString> list) {
		return new CharArrayStringBuffer(list);
	}
	
	@Override
	protected Class<CharArrayString> getArrayType() {
		return CharArrayString.class;
	}

	@Override
	public CharArrayString copy() {
		char[] copy = new char[length()];
		fill(copy, 0);
		return new CharArrayString(copy);
	}
	
	@Override
	public CharArray[] asCharBuffers() {
		if (strings == null) return new CharArray[0];
		CharArray[] chars = new CharArray[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = strings[i].asCharBuffer();
		return chars;
	}
	
	/** Create a readable CharacterStream from this string. */
	public ICharacterStream.Readable.Buffered asCharacterStream() {
		return new CS();
	}
	
	/** Create a writable CharacterStream from this string. */
	public ICharacterStream.Writable.Buffered asWritableCharacterStream() {
		return new WCS();
	}
	
	/** Base class for CharacterStream implementations. */
	@SuppressWarnings("squid:S2694") // not static because inherited classes are not static
	protected abstract class AbstractCS extends ConcurrentCloseable<IOException> implements ICharacterStream {
		protected Priority priority = Task.getCurrentPriority();
		
		@Override
		protected IAsync<IOException> closeUnderlyingResources() {
			return null;
		}
		
		@Override
		protected void closeResources(Async<IOException> ondone) {
			ondone.unblock();
		}

		@Override
		public Priority getPriority() {
			return priority;
		}
		
		@Override
		public void setPriority(Priority priority) {
			this.priority = priority;
		}
		
		@Override
		public String getDescription() {
			return "UnprotectedStringBuffer";
		}
		
		@Override
		public Charset getEncoding() {
			return StandardCharsets.UTF_16;
		}

	}
	
	/** CharacterStream implementation. */
	protected class CS extends AbstractCS implements ICharacterStream.Readable.Buffered {
		private int buffer = 0;
		private int bufferIndex = 0;
		private int back = -1;
		
		@Override
		public char read() throws EOFException {
			if (back != -1) {
				char c = (char)back;
				back = -1;
				return c;
			}
			if (strings == null) throw new EOFException();
			while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
				buffer++;
				bufferIndex = 0;
			}
			if (buffer > lastUsed) throw new EOFException();
			return strings[buffer].charAt(bufferIndex++);
		}
		
		@Override
		public int readSync(char[] buf, int offset, int length) {
			if (length <= 0) return 0;
			int done = 0;
			if (back != -1) {
				buf[offset++] = (char)back;
				back = -1;
				if (--length <= 0)
					return 1;
				if (strings == null) return 1;
				done = 1;
			} else if (strings == null) {
				return -1;
			}
			do {
				while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
					buffer++;
					bufferIndex = 0;
				}
				if (buffer > lastUsed) return done > 0 ? done : -1;
				int len = strings[buffer].length() - bufferIndex;
				if (len > length) len = length;
				System.arraycopy(strings[buffer].charArray(), strings[buffer].arrayStart() + bufferIndex, buf, offset, len);
				bufferIndex += len;
				offset += len;
				length -= len;
				done += len;
				if (length == 0)
					return done;
			} while (true);
		}
		
		@Override
		public int readAsync() {
			if (back != -1) {
				char c = (char)back;
				back = -1;
				return c;
			}
			if (strings == null) return -1;
			while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
				buffer++;
				bufferIndex = 0;
			}
			if (buffer > lastUsed) return -1;
			return strings[buffer].charAt(bufferIndex++);
		}
		
		@Override
		@SuppressWarnings("java:S1604") // cannot use lambda
		public AsyncSupplier<Integer, IOException> readAsync(char[] buf, int offset, int length) {
			return Task.cpu("UnprotectedStringBuffer.readAsync", priority, new Executable<Integer, IOException>() {
				@Override
				public Integer execute() {
					return Integer.valueOf(readSync(buf, offset, length));
				}
			}).start().getOutput();
		}
		
		@Override
		public AsyncSupplier<Chars.Readable, IOException> readNextBufferAsync() {
			return new AsyncSupplier<>(readNextBuffer(), null);
		}
		
		@Override
		public Chars.Readable readNextBuffer() {
			if (back != -1) {
				Chars.Readable s = new CharArray(new char[] { (char)back });
				back = -1;
				return s;
			}
			if (strings == null) return null;
			while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
				buffer++;
				bufferIndex = 0;
			}
			if (buffer > lastUsed) return null;
			CharArrayString str = strings[buffer].substring(bufferIndex);
			buffer++;
			bufferIndex = 0;
			return str.asCharBuffer();
		}
		
		@Override
		public boolean readUntil(char endChar, IString string) throws IOException {
			if (back != -1) {
				char c = (char)back;
				back = -1;
				if (c == endChar)
					return true;
				string.append(c);
			}
			if (strings == null) return false;
			do {
				while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
					buffer++;
					bufferIndex = 0;
				}
				if (buffer > lastUsed) return false;
				int pos = strings[buffer].indexOf(endChar, bufferIndex);
				if (pos >= 0) {
					if (pos > 0)
						string.append(strings[buffer].substring(bufferIndex, pos));
					bufferIndex = pos + 1;
					return true;
				}
				if (bufferIndex == 0)
					string.append(strings[buffer]);
				else
					string.append(strings[buffer].substring(bufferIndex, strings[buffer].length()));
				buffer++;
				bufferIndex = 0;
			} while (true);
		}
		
		@Override
		public AsyncSupplier<Boolean, IOException> readUntilAsync(char endChar, IString string) {
			AsyncSupplier<Boolean, IOException> result = new AsyncSupplier<>();
			Task.cpu("UnprotectedStringBuffer.readUntilAsync", getPriority(), new Executable<Void, NoException>() {
				@Override
				public Void execute() {
					try {
						result.unblockSuccess(Boolean.valueOf(readUntil(endChar, string)));
					} catch (IOException e) {
						result.error(e);
					}
					return null;
				}
			}).start();
			return result;
		}
		
		@Override
		public void back(char c) {
			back = c;
		}
		
		@Override
		public boolean endReached() {
			return back == -1 && (strings == null || buffer > lastUsed);
		}
		
		@Override
		public IAsync<IOException> canStartReading() {
			return new Async<>(true);
		}
	}
	
	/** CharacterStream implementation. */
	protected class WCS extends AbstractCS implements ICharacterStream.Writable.Buffered {

		@Override
		public void writeSync(char[] c, int offset, int length) {
			append(c, offset, length);
		}

		@Override
		public void writeSync(char c) throws IOException {
			append(c);
		}
		
		@Override
		@SuppressWarnings("java:S1604") // cannot use lambda
		public IAsync<IOException> writeAsync(char[] c, int offset, int length) {
			return Task.cpu("UnprotectedStringBuffer.writeAsync", priority, new Executable<Void, IOException>() {
				@Override
				public Void execute() {
					append(c, offset, length);
					return null;
				}
			}).start().getOutput();
		}

		@Override
		public IAsync<IOException> writeAsync(char c) {
			append(c);
			return new Async<>(true);
		}

		@Override
		public IAsync<IOException> flush() {
			return new Async<>(true);
		}

	}
	
	/** Encode this string with the given charset and write the result on the given writable IO. */
	public Async<IOException> encode(Charset charset, IO.Writable output, Priority priority) {
		if (strings == null) return new Async<>(true);
		Async<IOException> result = new Async<>();
		CharsetEncoder encoder = charset.newEncoder();
		encode(0, encoder, output, priority, null, result);
		return result;
	}
	
	private void encode(
		int index, CharsetEncoder encoder, IO.Writable output, Priority priority,
		IAsync<IOException> prevWrite, Async<IOException> result
	) {
		Task.cpu("Encode string into bytes", priority, () -> {
			try {
				ByteBuffer bytes = encoder.encode(strings[index].asCharBuffer().toCharBuffer());
				if (prevWrite == null || prevWrite.isDone()) {
					if (prevWrite != null && prevWrite.hasError()) {
						result.error(prevWrite.getError());
						return null;
					}
					IAsync<IOException> write = output.writeAsync(bytes);
					if (index == lastUsed) {
						write.onDone(result);
						return null;
					}
					encode(index + 1, encoder, output, priority, write, result);
					return null;
				}
				prevWrite.onDone(() -> {
					IAsync<IOException> write = output.writeAsync(bytes);
					if (index == lastUsed) {
						write.onDone(result);
						return;
					}
					encode(index + 1, encoder, output, priority, write, result);
				}, result);
			} catch (IOException e) {
				result.error(e);
			}
			return null;
		}).start();
	}

}
