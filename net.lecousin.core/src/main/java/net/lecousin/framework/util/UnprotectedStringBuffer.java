package net.lecousin.framework.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.UnaryOperator;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
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

	/** Create a string with the given one. */
	public UnprotectedStringBuffer(CharSequence s) {
		strings = new UnprotectedString[1];
		lastUsed = 0;
		strings[0] = new UnprotectedString(s);
	}
	
	/** Create a string with the given one. */
	public UnprotectedStringBuffer(IString s) {
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
	public boolean isEmpty() {
		if (strings == null) return true;
		for (int i = lastUsed; i >= 0; --i)
			if (!strings[i].isEmpty())
				return false;
		return true;
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
	public void setCharAt(int index, char c) {
		if (strings == null) throw new IllegalArgumentException("String is empty");
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (index < l) {
				strings[i].setCharAt(index, c);
				return;
			}
			index -= l;
		}
		throw new IllegalArgumentException("String is smaller than the given index");
	}
	
	@Override
	public UnprotectedStringBuffer append(char c) {
		if (strings == null) {
			strings = new UnprotectedString[8];
			strings[0] = new UnprotectedString(64).append(c);
			lastUsed = 0;
			return this;
		}
		if (strings[lastUsed].appendNoEnlarge(c))
			return this;
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = new UnprotectedString(64).append(c);
			return this;
		}
		UnprotectedString[] a = new UnprotectedString[++lastUsed + 8];
		System.arraycopy(strings, 0, a, 0, lastUsed);
		a[lastUsed] = new UnprotectedString(256).append(c);
		strings = a;
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer append(char[] chars, int offset, int len) {
		if (strings == null) {
			strings = new UnprotectedString[8];
			strings[0] = new UnprotectedString(len > 50 ? len + 64 : 64);
			strings[0].append(chars, offset, len);
			lastUsed = 0;
			return this;
		}
		int l = strings[lastUsed].canAppendWithoutEnlarging();
		if (l > 0) {
			if (l > len) l = len;
			strings[lastUsed].append(chars, offset, l);
			len -= l;
			if (len == 0)
				return this;
			offset += l;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = new UnprotectedString(len > 50 ? len + 128 : 64);
			strings[lastUsed].append(chars, offset, len);
			return this;
		}
		UnprotectedString[] a = new UnprotectedString[lastUsed + 1 + 8];
		System.arraycopy(strings, 0, a, 0, lastUsed + 1);
		a[++lastUsed] = new UnprotectedString(len > 50 ? len + 128 : 64);
		a[lastUsed].append(chars, offset, len);
		strings = a;
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer append(CharSequence s) {
		if (s == null) s = "null";
		if (s instanceof UnprotectedStringBuffer) {
			UnprotectedStringBuffer us = (UnprotectedStringBuffer)s;
			if (us.strings == null) return this;
			if (strings == null) {
				strings = new UnprotectedString[us.strings.length];
				System.arraycopy(us.strings, 0, strings, 0, strings.length);
				lastUsed = us.lastUsed;
				return this;
			}
			int i = 0;
			while (lastUsed < strings.length - 1 && i <= us.lastUsed) {
				strings[++lastUsed] = us.strings[i++];
			}
			if (i > us.lastUsed) return this;
			UnprotectedString[] a = new UnprotectedString[strings.length + (us.lastUsed - i + 1) + 8];
			System.arraycopy(strings, 0, a, 0, strings.length);
			System.arraycopy(us.strings, i, a, strings.length, us.lastUsed - i + 1);
			lastUsed = strings.length + (us.lastUsed - i + 1) - 1;
			strings = a;
			return this;
		}
		UnprotectedString us;
		if (s instanceof UnprotectedString)
			us = (UnprotectedString)s;
		else
			us = new UnprotectedString(s);
		if (strings == null) {
			strings = new UnprotectedString[8];
			strings[0] = us;
			lastUsed = 0;
			return this;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = us;
			return this;
		}
		UnprotectedString[] a = new UnprotectedString[strings.length + 8];
		System.arraycopy(strings, 0, a, 0, strings.length);
		a[strings.length] = us;
		lastUsed++;
		strings = a;
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer append(CharSequence s, int startPos, int endPos) {
		if (s == null) return append("null");
		if (s instanceof UnprotectedStringBuffer)
			return append(((UnprotectedStringBuffer)s).substring(startPos, endPos));
		if (s instanceof UnprotectedString)
			return append(((UnprotectedString)s).substring(startPos, endPos));
		return append(new UnprotectedString(s, startPos, endPos));
	}
	
	/** Add the given string at the beginning. */
	public UnprotectedStringBuffer addFirst(CharSequence s) {
		return addFirst(new UnprotectedString(s));
	}
	
	/** Add the given string at the beginning. */
	public UnprotectedStringBuffer addFirst(UnprotectedString s) {
		if (strings == null) {
			strings = new UnprotectedString[] { s, null, null, null, null, null, null, null };
			lastUsed = 0;
			return this;
		}
		if (lastUsed < strings.length - 1) {
			System.arraycopy(strings, 0, strings, 1, lastUsed + 1);
			strings[0] = s;
			lastUsed++;
			return this;
		}
		UnprotectedString[] ns = new UnprotectedString[lastUsed + 8];
		System.arraycopy(strings, 0, ns, 1, lastUsed + 1);
		ns[0] = s;
		strings = ns;
		lastUsed++;
		return this;
	}
	
	/** Add the given string at the beginning. */
	public UnprotectedStringBuffer addFirst(char c) {
		return addFirst(new UnprotectedString(c));
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
	@SuppressWarnings("squid:S3776") // complexity
	public int indexOf(CharSequence s, int start) {
		int sl = s.length();
		if (sl == 0 || strings == null) return -1;
		char first = s.charAt(0);
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (pos + l <= start) {
				pos += l;
				continue;
			}
			for (int j = pos < start ? start - pos : 0; j < l; ++j) {
				if (strings[i].charAt(j) != first) continue;
				int jj = j;
				int ii = i;
				int ll = l;
				int k;
				for (k = 1; k < sl; ++k) {
					if (++jj == ll) {
						if (++ii == lastUsed + 1) return -1;
						jj = 0;
						ll = strings[ii].length();
					}
					if (strings[ii].charAt(jj) != s.charAt(k))
						break;
				}
				if (k == sl) return pos + j;
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
	
	/** Compare this UnprotectedStringBuffer with another. */
	@SuppressWarnings("squid:S1201") // we want the name equals
	public boolean equals(UnprotectedStringBuffer s) {
		if (length() != s.length()) return false;
		if (strings == null) return true;
		int s1 = 0;
		int s2 = 0;
		int i1 = 0;
		int i2 = 0;
		int l1 = strings[0].length();
		int l2 = s.strings[0].length();
		do {
			while (i1 == l1) {
				s1++;
				if (s1 > lastUsed) return true;
				i1 = 0;
				l1 = strings[s1].length();
			}
			while (i2 == l2) {
				s2++;
				i2 = 0;
				l2 = s.strings[s2].length();
			}
			if (strings[s1].charAt(i1++) != s.strings[s2].charAt(i2++)) return false;
		} while (true);
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
	public int fillUsAsciiBytes(byte[] bytes, int start) {
		if (strings == null) return 0;
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i)
			pos += strings[i].fillUsAsciiBytes(bytes, start + pos);
		return pos;
	}
	
	@Override
	public UnprotectedStringBuffer trimBeginning() {
		if (strings != null)
			strings[0].trimBeginning();
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer trimEnd() {
		if (strings != null)
			strings[lastUsed].trimEnd();
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer removeStartChars(int nb) {
		while (strings != null) {
			int l = strings[0].length();
			if (nb < l) {
				strings[0].removeStartChars(nb);
				return this;
			}
			if (lastUsed == 0) {
				strings = null;
				return this;
			}
			System.arraycopy(strings, 1, strings, 0, lastUsed);
			strings[lastUsed] = null;
			lastUsed--;
			nb -= l;
		}
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer removeEndChars(int nb) {
		while (strings != null) {
			int l = strings[lastUsed].length();
			if (nb < l) {
				strings[lastUsed].removeEndChars(nb);
				return this;
			}
			if (lastUsed == 0) {
				strings = null;
				return this;
			}
			strings[lastUsed] = null;
			lastUsed--;
			nb -= l;
		}
		return this;
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
				if (current.isEmpty()) {
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
	
	@SuppressWarnings("squid:S3012") // false positive: Collections.addAll cannot be used on a subset of the array
	private void replace(int startBuffer, int startBufferIndex, int endBuffer, int endBufferIndex, UnprotectedStringBuffer replace) {
		ArrayList<UnprotectedString> list = new ArrayList<>(
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
	
	/** Replace all occurrences of search into replace. */
	public UnprotectedStringBuffer replace(CharSequence search, UnprotectedString replace) {
		int pos = 0;
		while ((pos = indexOf(search, pos)) >= 0) {
			replace(pos, pos + search.length() - 1, replace);
			pos = pos + replace.length();
		}
		return this;
	}
	
	/** Replace all occurrences of search into replace. */
	public UnprotectedStringBuffer replace(CharSequence search, CharSequence replace) {
		return replace(search, new UnprotectedString(replace));
	}
	
	@Override
	public UnprotectedStringBuffer replace(char oldChar, char newChar) {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].replace(oldChar, newChar);
		return this;
	}
	
	/** Replace all occurrences of oldChar into replaceValue. */
	public UnprotectedStringBuffer replace(char oldChar, CharSequence replaceValue) {
		return replace(oldChar, new UnprotectedString(replaceValue));
	}

	/** Replace all occurrences of oldChar into replaceValue. */
	public UnprotectedStringBuffer replace(char oldChar, UnprotectedString replaceValue) {
		if (replaceValue.length() == 1)
			return replace(oldChar, replaceValue.charAt(0));
		if (strings == null)
			return this;
		int pos = 0;
		while ((pos = indexOf(oldChar, pos)) >= 0) {
			replace(pos, pos, replaceValue);
			pos = pos + replaceValue.length();
		}
		return this;
	}
	
	/** Remove characters from start to end (inclusive), and replace them by the given single character. */
	public void replace(int start, int end, char c) {
		replace(start, end, new char[] { c });
	}
	
	/** Remove characters from start to end (inclusive), and replace them by the given characters. */
	public void replace(int start, int end, char[] chars) {
		replace(start, end, new UnprotectedString(chars));
	}
	
	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	public void replace(int start, int end, UnprotectedString s) {
		if (strings == null) return;
		if (end < start) return;
		int firstBufferIndex = 0;
		int firstBufferPos = 0;
		int firstBufferLen;
		do {
			firstBufferLen = strings[firstBufferIndex].length();
			if (start < firstBufferPos + firstBufferLen) break;
			firstBufferPos += firstBufferLen;
			if (++firstBufferIndex > lastUsed) return;
		} while (true);
		int lastBufferIndex = firstBufferIndex;
		int lastBufferPos = firstBufferPos;
		int lastBufferLen = firstBufferLen;
		while (end >= lastBufferPos + lastBufferLen) {
			if (++lastBufferIndex > lastUsed) {
				lastBufferIndex--;
				end = lastBufferPos + lastBufferLen - 1;
				break;
			}
			lastBufferPos += lastBufferLen;
			lastBufferLen = strings[lastBufferIndex].length();
		}
		replaceStrings(firstBufferIndex, lastBufferIndex,
			// remaining part on the fist buffer
			start == firstBufferPos ? null : strings[firstBufferIndex].substring(0, start - firstBufferPos),
			// remaining part of the last buffer
			end == lastBufferPos + lastBufferLen - 1 ? null : strings[lastBufferIndex].substring(end - lastBufferPos + 1),
			// to insert
			1,
			s
		);
	}

	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	public UnprotectedStringBuffer replace(int start, int end, UnprotectedStringBuffer s) {
		if (strings == null) return this;
		if (end < start) return this;
		int firstBufferIndex = 0;
		int firstBufferPos = 0;
		int firstBufferLen;
		do {
			firstBufferLen = strings[firstBufferIndex].length();
			if (start < firstBufferPos + firstBufferLen) break;
			firstBufferPos += firstBufferLen;
			if (++firstBufferIndex > lastUsed) return this;
		} while (true);
		int lastBufferIndex = firstBufferIndex;
		int lastBufferPos = firstBufferPos;
		int lastBufferLen = firstBufferLen;
		while (end >= lastBufferPos + lastBufferLen) {
			if (++lastBufferIndex > lastUsed) {
				lastBufferIndex--;
				end = lastBufferPos + lastBufferLen - 1;
				break;
			}
			lastBufferPos += lastBufferLen;
			lastBufferLen = strings[lastBufferIndex].length();
		}
		replaceStrings(firstBufferIndex, lastBufferIndex,
			// remaining part on the fist buffer
			start == firstBufferPos ? null : strings[firstBufferIndex].substring(0, start - firstBufferPos),
			// remaining part of the last buffer
			end == lastBufferPos + lastBufferLen - 1 ? null : strings[lastBufferIndex].substring(end - lastBufferPos + 1),
			// to insert
			s.strings == null ? 0 : s.lastUsed + 1,
			s.strings
		);
		return this;
	}
	
	private void replaceStrings(int startIndex, int endIndex,
		UnprotectedString first, UnprotectedString last, int nbMiddle, UnprotectedString... middle) {
		int nb = startIndex + (lastUsed - endIndex) + (first != null ? 1 : 0) + (last != null ? 1 : 0) + nbMiddle;
		if (nb <= strings.length) {
			int pos;
			// put the strings after endIndex
			if (endIndex < lastUsed) {
				System.arraycopy(strings, endIndex + 1, strings, nb - (lastUsed - endIndex), lastUsed - endIndex);
				pos = nb - (lastUsed - endIndex) - 1;
			} else {
				pos = nb - 1;
			}
			// put the last string
			if (last != null)
				strings[pos--] = last;
			for (int i = nbMiddle - 1; i >= 0; --i)
				strings[pos--] = middle[i];
			if (first != null)
				strings[pos] = first;
			for (int i = nb; i <= lastUsed; ++i)
				strings[i] = null;
			lastUsed = nb - 1;
			return;
		}
		UnprotectedString[] list = new UnprotectedString[nb + 3];
		int pos = 0;
		if (startIndex > 0) {
			System.arraycopy(strings, 0, list, 0, startIndex);
			pos = startIndex;
		}
		if (first != null)
			list[pos++] = first;
		for (int i = 0; i < nbMiddle; ++i)
			list[pos++] = middle[i];
		if (last != null)
			list[pos++] = last;
		if (endIndex < lastUsed)
			System.arraycopy(strings, endIndex + 1, list, pos, lastUsed - endIndex);
		strings = list;
		lastUsed = nb - 1;
	}

	/** Search for a starting string and a ending string, and replace them including the content with new content.
	 * This may be typically used to replace variables such as ${xxx} with their values.
	 */
	@SuppressWarnings("squid:S3776") // complexity
	public void searchAndReplace(
		CharSequence start, CharSequence end, UnaryOperator<UnprotectedStringBuffer> valueProvider
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
						UnprotectedStringBuffer value = valueProvider.apply(variable);
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
					bufferIndex++;
				}
				continue;
			}
			startChar = start.charAt(++startIndex);
			bufferIndex++;
		}
	}

	@Override
	public UnprotectedStringBuffer toLowerCase() {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].toLowerCase();
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer toUpperCase() {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].toUpperCase();
		return this;
	}
	
	@Override
	public boolean startsWith(CharSequence start) {
		if (strings == null) return start.length() == 0;
		int l = start.length();
		int stringIndex = 0;
		int stringPos = 0;
		int stringLen = strings[0].length();
		int i = 0;
		while (i < l) {
			if (strings[stringIndex].charAt(stringPos) != start.charAt(i))
				return false;
			if (i == l - 1) return true;
			if (++stringPos == stringLen) {
				if (++stringIndex > lastUsed)
					return false;
				stringPos = 0;
				stringLen = strings[stringIndex].length();
			}
			i++;
		}
		return true;
	}
	
	@Override
	public boolean endsWith(CharSequence end) {
		if (strings == null) return end.length() == 0;
		int stringIndex = lastUsed;
		int stringPos = strings[stringIndex].length() - 1;
		int i = end.length() - 1;
		while (i >= 0) {
			if (strings[stringIndex].charAt(stringPos) != end.charAt(i))
				return false;
			if (i == 0) return true;
			if (--stringPos < 0) {
				if (--stringIndex < 0)
					return false;
				stringPos = strings[stringIndex].length() - 1;
			}
			i--;
		}
		return true;
	}
	
	@Override
	public String toString() {
		if (strings == null) return "";
		if (lastUsed == 0) return strings[0].toString();
		char[] chars = new char[length()];
		fill(chars, 0);
		return new String(chars);
	}
	
	@Override
	public char[][] asCharacters() {
		if (strings == null) return new char[][] {};
		char[][] chars = new char[lastUsed + 1][];
		for (int i = 0; i <= lastUsed; ++i) {
			chars[i] = new char[strings[i].length()];
			strings[i].fill(chars[i]);
		}
		return chars;
	}
	
	@Override
	public CharBuffer[] asCharBuffers() {
		if (strings == null) return new CharBuffer[0];
		CharBuffer[] chars = new CharBuffer[lastUsed + 1];
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
		protected byte priority = Task.PRIORITY_NORMAL;
		
		@Override
		protected IAsync<IOException> closeUnderlyingResources() {
			return null;
		}
		
		@Override
		protected void closeResources(Async<IOException> ondone) {
			ondone.unblock();
		}

		@Override
		public byte getPriority() {
			return priority;
		}
		
		@Override
		public void setPriority(byte priority) {
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
				System.arraycopy(strings[buffer].charArray(), strings[buffer].charArrayStart() + bufferIndex, buf, offset, len);
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
		public AsyncSupplier<Integer, IOException> readAsync(char[] buf, int offset, int length) {
			return new Task.Cpu<Integer, IOException>("UnprotectedStringBuffer.readAsync", priority) {
				@Override
				public Integer run() {
					return Integer.valueOf(readSync(buf, offset, length));
				}
			}.start().getOutput();
		}
		
		@Override
		public AsyncSupplier<UnprotectedString, IOException> readNextBufferAsync() {
			return new AsyncSupplier<>(readNextBuffer(), null);
		}
		
		@Override
		public UnprotectedString readNextBuffer() {
			if (back != -1) {
				UnprotectedString s = new UnprotectedString(new char[] { (char)back }, 0, 1, 1);
				back = -1;
				return s;
			}
			if (strings == null) return null;
			while (buffer <= lastUsed && bufferIndex == strings[buffer].length()) {
				buffer++;
				bufferIndex = 0;
			}
			if (buffer > lastUsed) return null;
			UnprotectedString str = strings[buffer].substring(bufferIndex);
			buffer++;
			bufferIndex = 0;
			return str;
		}
		
		@Override
		public boolean readUntil(char endChar, UnprotectedStringBuffer string) throws IOException {
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
		public AsyncSupplier<Boolean, IOException> readUntilAsync(char endChar, UnprotectedStringBuffer string) {
			AsyncSupplier<Boolean, IOException> result = new AsyncSupplier<>();
			new Task.Cpu.FromRunnable("UnprotectedStringBuffer.readUntilAsync", getPriority(), () -> {
				try {
					result.unblockSuccess(Boolean.valueOf(readUntil(endChar, string)));
				} catch (IOException e) {
					result.error(e);
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
		public IAsync<IOException> writeAsync(char[] c, int offset, int length) {
			return new Task.Cpu<Void, IOException>("UnprotectedStringBuffer.writeAsync", priority) {
				@Override
				public Void run() throws IOException, CancelException {
					append(c, offset, length);
					return null;
				}
			}.start().getOutput();
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
	public Async<IOException> encode(Charset charset, IO.Writable output, byte priority) {
		if (strings == null) return new Async<>(true);
		Async<IOException> result = new Async<>();
		CharsetEncoder encoder = charset.newEncoder();
		encode(0, encoder, output, priority, null, result);
		return result;
	}
	
	private void encode(
		int index, CharsetEncoder encoder, IO.Writable output, byte priority,
		IAsync<IOException> prevWrite, Async<IOException> result
	) {
		new Task.Cpu.FromRunnable("Encode string into bytes", priority, () -> {
			try {
				ByteBuffer bytes = encoder.encode(strings[index].asCharBuffer());
				if (prevWrite == null || prevWrite.isDone()) {
					if (prevWrite != null && prevWrite.hasError()) {
						result.error(prevWrite.getError());
						return;
					}
					IAsync<IOException> write = output.writeAsync(bytes);
					if (index == lastUsed) {
						write.onDone(result);
						return;
					}
					encode(index + 1, encoder, output, priority, write, result);
					return;
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
		}).start();
	}
}
