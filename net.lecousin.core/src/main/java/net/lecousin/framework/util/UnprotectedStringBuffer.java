package net.lecousin.framework.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

/**
 * Array of UnprotectedString, allowing to add and remove characters without re-allocating a character array.
 * 
 * @see UnprotectedString
 */
public class UnprotectedStringBuffer implements IString {

	/** Create a new empty string. */
	public UnprotectedStringBuffer() {
		strings = null;
		lastUsed = 0;
	}

	/** Create a string with the given one. */
	public UnprotectedStringBuffer(UnprotectedString string) {
		strings = new UnprotectedString[8];
		strings[0] = string;
		lastUsed = 0;
	}
	
	/** Create a string with the given ones. */
	public UnprotectedStringBuffer(Collection<UnprotectedString> strings) {
		this.strings = new UnprotectedString[strings.size() + 8];
		int i = 0;
		for (UnprotectedString s : strings) this.strings[i++] = s;
		lastUsed = i - 1;
	}
	
	/** Create a string with the given one. */
	public UnprotectedStringBuffer(String s) {
		strings = new UnprotectedString[1];
		lastUsed = 0;
		strings[0] = new UnprotectedString(s);
	}
	
	private UnprotectedString[] strings;
	private int lastUsed;
	
	/** Return the number of UnprotectedString hold by this instance that can be used. */
	public int getNbUsableUnprotectedStrings() {
		if (strings == null) return 0;
		return lastUsed + 1;
	}
	
	/** Return a specific UnprotectedString hold by this instance. */
	public UnprotectedString getUnprotectedString(int index) {
		return strings[index];
	}
	
	@Override
	public int length() {
		if (strings == null) return 0;
		int len = 0;
		for (int i = lastUsed; i >= 0; --i)
			len += strings[i].length();
		return len;
	}
	
	@Override
	public char charAt(int index) {
		if (strings == null) return 0;
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (index < l) return strings[i].charAt(index);
			index -= l;
		}
		return 0;
	}
	
	@Override
	public void append(char c) {
		if (strings == null) {
			strings = new UnprotectedString[8];
			strings[0] = new UnprotectedString(64);
			strings[0].append(c);
			lastUsed = 0;
			return;
		}
		if (strings[lastUsed].canAppendWithoutEnlarging() > 0) {
			strings[lastUsed].append(c);
			return;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = new UnprotectedString(64);
			strings[lastUsed].append(c);
			return;
		}
		UnprotectedString[] a = new UnprotectedString[lastUsed + 1 + 8];
		System.arraycopy(strings, 0, a, 0, lastUsed + 1);
		a[++lastUsed] = new UnprotectedString(64);
		a[lastUsed].append(c);
		strings = a;
	}
	
	@Override
	public void append(char[] chars, int offset, int len) {
		if (strings == null) {
			strings = new UnprotectedString[8];
			strings[0] = new UnprotectedString(len > 50 ? len + 64 : 64);
			strings[0].append(chars, offset, len);
			lastUsed = 0;
			return;
		}
		int l = strings[lastUsed].canAppendWithoutEnlarging();
		if (l > 0) {
			if (l > len) l = len;
			strings[lastUsed].append(chars, offset, l);
			len -= l;
			if (len == 0)
				return;
			offset += l;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = new UnprotectedString(len > 50 ? len + 64 : 64);
			strings[lastUsed].append(chars, offset, len);
			return;
		}
		UnprotectedString[] a = new UnprotectedString[lastUsed + 1 + 8];
		System.arraycopy(strings, 0, a, 0, lastUsed + 1);
		a[++lastUsed] = new UnprotectedString(len > 50 ? len + 64 : 64);
		a[lastUsed].append(chars, offset, len);
		strings = a;
	}
	
	@Override
	public void append(CharSequence s) {
		if (s instanceof UnprotectedStringBuffer) {
			UnprotectedStringBuffer us = (UnprotectedStringBuffer)s;
			if (us.strings == null) return;
			if (strings == null) {
				strings = new UnprotectedString[us.strings.length];
				System.arraycopy(us.strings, 0, strings, 0, strings.length);
				lastUsed = us.lastUsed;
				return;
			}
			int i = 0;
			while (lastUsed < strings.length - 1 && i <= us.lastUsed) {
				strings[++lastUsed] = us.strings[i++];
			}
			if (i > us.lastUsed) return;
			UnprotectedString[] a = new UnprotectedString[strings.length + (us.lastUsed - i + 1) + 8];
			System.arraycopy(strings, 0, a, 0, strings.length);
			System.arraycopy(us.strings, i, a, strings.length, us.lastUsed - i + 1);
			lastUsed = strings.length + (us.lastUsed - i + 1) - 1;
			strings = a;
			return;
		}
		if (s instanceof UnprotectedString) {
			UnprotectedString us = (UnprotectedString)s;
			if (strings == null) {
				strings = new UnprotectedString[8];
				strings[0] = us;
				lastUsed = 0;
				return;
			}
			if (lastUsed < strings.length - 1) {
				strings[++lastUsed] = us;
				return;
			}
			UnprotectedString[] a = new UnprotectedString[strings.length + 8];
			System.arraycopy(strings, 0, a, 0, strings.length);
			a[strings.length] = us;
			lastUsed++;
			strings = a;
			return;
		}
		int l = s.length();
		for (int i = 0; i < l; ++i)
			append(s.charAt(i));
	}
	
	@Override
	public int indexOf(char c, int start) {
		if (strings == null) return -1;
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (pos + l <= start) {
				pos += l;
				continue;
			}
			int j;
			if (pos < start)
				j = strings[i].indexOf(c, start - pos);
			else
				j = strings[i].indexOf(c);
			if (j >= 0) return j + pos;
			pos += l;
		}
		return -1;
	}
	
	@Override
	public int indexOf(CharSequence s, int start) {
		int sl = s.length();
		if (sl == 0) return -1;
		if (strings == null) return -1;
		char first = s.charAt(0);
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (pos + l <= start) {
				pos += l;
				continue;
			}
			for (int j = pos < start ? start - pos : 0; j < l; ++j) {
				if (strings[i].charAt(j) == first) {
					int jj = j;
					int ii = i;
					int ll = l;
					int k;
					for (k = 1; k < sl; ++k) {
						jj++;
						if (jj == ll) {
							ii++;
							if (ii == lastUsed + 1) return -1;
							jj = 0;
							ll = strings[ii].length();
						}
						if (strings[ii].charAt(jj) != s.charAt(k))
							break;
					}
					if (k == sl) return pos + j;
				}
			}
			pos += l;
		}
		return -1;
	}
	
	@Override
	public StringBuilder subSequence(int start, int end) {
		StringBuilder s = new StringBuilder();
		if (strings == null) return s;
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (start < pos + l) {
				int j = end - pos;
				if (j > l) j = l;
				s.append(strings[i].subSequence(start - pos, j));
				start = pos + j;
				if (start >= end) break;
			}
			pos += l;
		}
		return s;
	}
	
	@Override
	public UnprotectedStringBuffer substring(int start) {
		if (strings == null) return this;
		int pos = 0;
		LinkedList<UnprotectedString> list = new LinkedList<>();
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (start < pos + l) {
				list.add(strings[i].substring(start - pos, l));
				start = pos + l;
			}
			pos += l;
		}
		return new UnprotectedStringBuffer(list);
	}

	@Override
	public UnprotectedStringBuffer substring(int start, int end) {
		if (strings == null) return this;
		int pos = 0;
		LinkedList<UnprotectedString> list = new LinkedList<>();
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (start < pos + l) {
				int j = end - pos;
				if (j > l) j = l;
				list.add(strings[i].substring(start - pos, j));
				start = pos + j;
				if (start >= end) break;
			}
			pos += l;
		}
		return new UnprotectedStringBuffer(list);
	}
	
	@Override
	public int fill(char[] chars, int start) {
		if (strings == null) return 0;
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i)
			pos += strings[i].fill(chars, start + pos);
		return pos;
	}
	
	@Override
	public void trimBeginning() {
		if (strings == null) return;
		strings[0].trimBeginning();
	}
	
	@Override
	public void trimEnd() {
		if (strings == null) return;
		strings[lastUsed].trimEnd();
	}
	
	@Override
	public LinkedList<UnprotectedStringBuffer> split(char sep) {
		LinkedList<UnprotectedStringBuffer> list = new LinkedList<>();
		if (strings == null) return list;
		int index = 0;
		UnprotectedStringBuffer current = new UnprotectedStringBuffer();
		while (index <= lastUsed) {
			int pos = 0;
			do {
				int i = strings[index].indexOf(sep, pos);
				if (i < 0) {
					current.append(strings[index].substring(pos, strings[index].length()));
					break;
				}
				if (current.length() == 0) {
					list.add(new UnprotectedStringBuffer(strings[index].substring(pos, i)));
				} else {
					current.append(strings[index].substring(pos, i));
					list.add(current);
					current = new UnprotectedStringBuffer();
				}
				pos = i + 1;
			} while (pos < strings[index].length());
			index++;
		}
		if (current.length() > 0)
			list.add(current);
		return list;
	}
	
	/** start and end are inclusive. */
	private UnprotectedStringBuffer subBuffer(int startBuffer, int startBufferIndex, int endBuffer, int endBufferIndex) {
		UnprotectedStringBuffer result = new UnprotectedStringBuffer();
		result.strings = new UnprotectedString[endBuffer - startBuffer + 1];
		result.lastUsed = result.strings.length - 1;
		for (int buffer = startBuffer; buffer <= endBuffer; ++buffer) {
			int start = buffer == startBuffer ? startBufferIndex : 0;
			int end = buffer == endBuffer ? endBufferIndex : strings[buffer].length();
			result.strings[buffer - startBuffer] = strings[buffer].substring(start, end);
		}
		return result;
	}
	
	private void replace(int startBuffer, int startBufferIndex, int endBuffer, int endBufferIndex, UnprotectedStringBuffer replace) {
		ArrayList<UnprotectedString> list = new ArrayList<UnprotectedString>(
			startBuffer + 1 + replace.lastUsed + 1 + lastUsed - endBuffer + 1
		);
		// add all strings before start
		for (int i = 0; i < startBuffer; ++i)
			list.add(strings[i]);
		// add start if necessary
		if (startBufferIndex > 0)
			list.add(strings[startBuffer].substring(0, startBufferIndex));
		// add replace
		if (replace.strings != null)
			for (int i = 0; i <= replace.lastUsed; ++i)
				list.add(replace.strings[i]);
		// add end if necessary
		if (endBufferIndex < strings[endBuffer].length() - 1)
			list.add(strings[endBuffer].substring(endBufferIndex + 1));
		// add all strings after end
		for (int i = endBuffer + 1; i <= lastUsed; ++i)
			list.add(strings[i]);
		
		strings = list.toArray(new UnprotectedString[list.size()]);
		lastUsed = strings.length - 1;
	}
	
	/** Search for a starting string and a ending string, and replace them including the content with new content.
	 * This may be typically used to replace variables such as ${xxx} with their values.
	 */
	public void searchAndReplace(
		CharSequence start, CharSequence end, Provider.FromValue<UnprotectedStringBuffer, UnprotectedStringBuffer> valueProvider
	) {
		if (strings == null) return;
		int buffer = 0;
		int bufferIndex = 0;
		int startIndex = 0;
		char startChar = start.charAt(0);
		int startOfStartBuffer = 0;
		int startOfStartBufferIndex = 0;
		while (buffer <= lastUsed) {
			if (bufferIndex >= strings[buffer].length()) {
				buffer++;
				bufferIndex = 0;
				continue;
			}
			char c = strings[buffer].charAt(bufferIndex);
			if (c != startChar) {
				if (startIndex > 0) {
					startIndex = 0;
					startChar = start.charAt(0);
				}
				bufferIndex++;
				continue;
			}
			// start character found
			if (startIndex == 0) {
				startOfStartBuffer = buffer;
				startOfStartBufferIndex = bufferIndex;
			}
			if (startIndex == start.length() - 1) {
				// start fully found
				// reinit next search
				startIndex = 0;
				startChar = start.charAt(0);
				// search for end
				int endIndex = 0;
				char endChar = end.charAt(0);
				int endOfStartBuffer = buffer;
				int endOfStartBufferIndex = bufferIndex;
				bufferIndex++;
				while (buffer <= lastUsed) {
					if (bufferIndex >= strings[buffer].length()) {
						buffer++;
						bufferIndex = 0;
						continue;
					}
					c = strings[buffer].charAt(bufferIndex);
					if (c != endChar) {
						if (endIndex > 0) {
							endIndex = 0;
							endChar = end.charAt(0);
						}
						bufferIndex++;
						continue;
					}
					// end character found
					if (endIndex == end.length() - 1) {
						// end fully found
						// variable starts at character after start
						endOfStartBufferIndex++;
						if (endOfStartBufferIndex == strings[endOfStartBuffer].length()) {
							endOfStartBuffer++;
							endOfStartBufferIndex = 0;
						}
						// and ends at character before end
						int b = buffer;
						int i = bufferIndex - end.length() + 1;
						while (i < 0) {
							b--;
							i += strings[b].length();
						}
						UnprotectedStringBuffer variable = subBuffer(endOfStartBuffer, endOfStartBufferIndex, b, i);
						UnprotectedStringBuffer value = valueProvider.provide(variable);
						replace(startOfStartBuffer, startOfStartBufferIndex, buffer, bufferIndex, value);
						bufferIndex++;
						bufferIndex -= start.length() + end.length() + variable.length();
						bufferIndex += value.length();
						while (bufferIndex < 0) {
							buffer--;
							bufferIndex += strings[buffer].length();
						}
						while (buffer <= lastUsed && bufferIndex >= strings[buffer].length()) {
							bufferIndex -= strings[buffer].length();
							buffer++;
						}
						break;
					}
					endChar = end.charAt(++endIndex);
				}
				continue;
			}
			startChar = start.charAt(++startIndex);
			bufferIndex++;
		}
	}

	@Override
	public void toLowerCase() {
		if (strings == null) return;
		for (int i = 0; i <= lastUsed; ++i)
			strings[i].toLowerCase();
	}
	
	@Override
	public void toUpperCase() {
		if (strings == null) return;
		for (int i = 0; i <= lastUsed; ++i)
			strings[i].toUpperCase();
	}

	@Override
	public String toString() {
		if (strings == null) return "";
		if (lastUsed == 0) return strings[0].toString();
		char[] chars = new char[length()];
		fill(chars, 0);
		return new String(chars);
	}
	
	/** Create a readable CharacterStream from this string. */
	public ICharacterStream.Readable.Buffered asCharacterStream() {
		return new ICharacterStream.Readable.Buffered() {
			private int buffer = 0;
			private int bufferIndex = 0;
			@Override
			public String getSourceDescription() {
				return "UnprotectedStringBuffer";
			}
			
			@Override
			public char read() throws EOFException {
				if (strings == null) throw new EOFException();
				while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
					buffer++;
					bufferIndex = 0;
				}
				if (buffer > lastUsed) throw new EOFException();
				return strings[buffer].charAt(bufferIndex++);
			}
			
			@Override
			public int read(char[] buf, int offset, int length) {
				if (strings == null) return -1;
				int done = 0;
				do {
					while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
						buffer++;
						bufferIndex = 0;
					}
					if (buffer > lastUsed) return done > 0 ? done : -1;
					int len = strings[buffer].length() - bufferIndex;
					if (len > length) len = length;
					System.arraycopy(strings[buffer], bufferIndex, buf, offset, len);
					offset += len;
					length -= len;
					done += len;
					if (length == 0)
						return done;
				} while (true);
			}
			
			@Override
			public void back(char c) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean endReached() {
				return strings == null || buffer > lastUsed;
			}
			
			@Override
			public void close() {}
			
			@Override
			public ISynchronizationPoint<IOException> closeAsync() {
				return new SynchronizationPoint<>(true);
			}
			
			@Override
			public ISynchronizationPoint<IOException> canStartReading() {
				return new SynchronizationPoint<>(true);
			}
		};
	}
	
	/** Encode this string with the given charset and write the result on the given writable IO. */
	public AsyncWork<Void,IOException> encode(Charset charset, IO.Writable output, byte priority) {
		Task<Void,IOException> task = new Task.Cpu<Void,IOException>("Encode string into bytes", priority) {
			@Override
			public Void run() throws IOException {
				CharsetEncoder encoder = charset.newEncoder();
				for (int i = 0; i <= lastUsed; ++i) {
					ByteBuffer bytes = encoder.encode(strings[i].asCharBuffer());
					output.writeAsync(bytes);
				}
				return null;
			}
		};
		task.start();
		return task.getSynch();
	}
}
