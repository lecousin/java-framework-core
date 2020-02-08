package net.lecousin.framework.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Array of ArrayString, allowing to add and remove characters without re-allocating an array.
 * 
 * @param <T> type of ArrayString
 * @param <ME> my final type
 * @see ArrayString
 */
public abstract class ArrayStringBuffer<T extends ArrayString, ME extends ArrayStringBuffer<T, ME>> implements IString {

	protected T[] strings;
	protected int lastUsed;
	protected int newArrayStringCapacity = 64;

	/** Create a new empty string. */
	public ArrayStringBuffer() {
		strings = null;
		lastUsed = 0;
	}

	/** Create a string with the given one. */
	public ArrayStringBuffer(T string) {
		strings = allocateArray(8);
		strings[0] = string;
		lastUsed = 0;
	}
	
	/** Create a string with the given ones. */
	public ArrayStringBuffer(Collection<T> strings) {
		this.strings = allocateArray(strings.size() + 8);
		int i = 0;
		for (T s : strings) this.strings[i++] = s;
		lastUsed = i - 1;
	}
	
	public void setNewArrayStringCapacity(int capacity) {
		newArrayStringCapacity = capacity;
	}
	
	protected abstract T[] allocateArray(int arraySize);
	
	protected abstract T createString(int initialCapacity);
	
	protected abstract T createString(CharSequence s);
	
	protected abstract T createString(CharSequence s, int startPos, int endPos);
	
	protected abstract ME createBuffer();
	
	protected abstract ME createBuffer(T s);
	
	protected abstract ME createBuffer(List<T> list);
	
	protected abstract Class<T> getArrayType();
	
	/** Reset this string to empty. */
	public void reset() {
		strings = null;
		lastUsed = 0;
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
	
	@SuppressWarnings("unchecked")
	@Override
	public ME append(char c) {
		if (strings == null) {
			strings = allocateArray(8);
			strings[0] = createString(newArrayStringCapacity);
			strings[0].append(c);
			lastUsed = 0;
			return (ME)this;
		}
		if (strings[lastUsed].appendNoEnlarge(c))
			return (ME)this;
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = createString(newArrayStringCapacity);
			strings[lastUsed].append(c);
			return (ME)this;
		}
		T[] a = allocateArray(++lastUsed + 8);
		System.arraycopy(strings, 0, a, 0, lastUsed);
		a[lastUsed] = createString(newArrayStringCapacity);
		a[lastUsed].append(c);
		strings = a;
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME append(char[] chars, int offset, int len) {
		if (strings == null) {
			strings = allocateArray(8);
			strings[0] = createString(len + newArrayStringCapacity);
			strings[0].append(chars, offset, len);
			lastUsed = 0;
			return (ME)this;
		}
		int l = strings[lastUsed].canAppendWithoutEnlarging();
		if (l > 0) {
			if (l > len) l = len;
			strings[lastUsed].append(chars, offset, l);
			len -= l;
			if (len == 0)
				return (ME)this;
			offset += l;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = createString(len + newArrayStringCapacity);
			strings[lastUsed].append(chars, offset, len);
			return (ME)this;
		}
		T[] a = allocateArray(lastUsed + 1 + 8);
		System.arraycopy(strings, 0, a, 0, lastUsed + 1);
		a[++lastUsed] = createString(len + newArrayStringCapacity);
		a[lastUsed].append(chars, offset, len);
		strings = a;
		return (ME)this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ME append(CharSequence s) {
		if (s == null) s = "null";
		if (s instanceof ArrayStringBuffer && ((ArrayStringBuffer<?,?>)s).getArrayType().equals(getArrayType())) {
			ME us = (ME)s;
			if (us.strings == null) return (ME)this;
			if (strings == null) {
				strings = allocateArray(us.strings.length);
				System.arraycopy(us.strings, 0, strings, 0, strings.length);
				lastUsed = us.lastUsed;
				return (ME)this;
			}
			int i = 0;
			while (lastUsed < strings.length - 1 && i <= us.lastUsed) {
				strings[++lastUsed] = us.strings[i++];
			}
			if (i > us.lastUsed) return (ME)this;
			T[] a = allocateArray(strings.length + (us.lastUsed - i + 1) + 8);
			System.arraycopy(strings, 0, a, 0, strings.length);
			System.arraycopy(us.strings, i, a, strings.length, us.lastUsed - i + 1);
			lastUsed = strings.length + (us.lastUsed - i + 1) - 1;
			strings = a;
			return (ME)this;
		}
		T us;
		if (!s.getClass().equals(getArrayType())) {
			int len = s.length();
			if (strings != null) {
				int remaining = strings[lastUsed].canAppendWithoutEnlarging();
				if (len <= remaining) {
					strings[lastUsed].append(s);
					return (ME)this;
				}
			}
			if (len < newArrayStringCapacity) {
				us = createString(newArrayStringCapacity);
				us.append(s);
			} else {
				us = createString(s);
			}
		} else {
			us = (T)s;
		}
		if (strings == null) {
			strings = allocateArray(8);
			strings[0] = us;
			lastUsed = 0;
			return (ME)this;
		}
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = us;
			return (ME)this;
		}
		T[] a = allocateArray(strings.length + 8);
		System.arraycopy(strings, 0, a, 0, strings.length);
		a[strings.length] = us;
		lastUsed++;
		strings = a;
		return (ME)this;
	}
	
	@Override
	public ME append(CharSequence s, int startPos, int endPos) {
		if (s == null) return append("null");
		if (s instanceof IString)
			return append(((IString)s).substring(startPos, endPos));
		return append(createString(s, startPos, endPos));
	}
	
	/** Add the given string at the beginning. */
	public ME addFirst(CharSequence s) {
		return addFirst(createString(s));
	}
	
	/** Add the given string at the beginning. */
	@SuppressWarnings("unchecked")
	public ME addFirst(T s) {
		if (strings == null) {
			strings = allocateArray(8);
			strings[0] = s;
			lastUsed = 0;
			return (ME)this;
		}
		if (lastUsed < strings.length - 1) {
			System.arraycopy(strings, 0, strings, 1, lastUsed + 1);
			strings[0] = s;
			lastUsed++;
			return (ME)this;
		}
		T[] ns = allocateArray(lastUsed + 8);
		System.arraycopy(strings, 0, ns, 1, lastUsed + 1);
		ns[0] = s;
		strings = ns;
		lastUsed++;
		return (ME)this;
	}
	
	/** Add the given character at the beginning. */
	public ME addFirst(char c) {
		return addFirst(createString(newArrayStringCapacity).append(c));
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
	@SuppressWarnings("java:S3776") // complexity
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
	
	@SuppressWarnings("unchecked")
	@Override
	public ME substring(int start) {
		if (strings == null) return (ME)this;
		int pos = 0;
		LinkedList<T> list = new LinkedList<>();
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (start < pos + l) {
				list.add((T)strings[i].substring(start - pos, l));
				start = pos + l;
			}
			pos += l;
		}
		return createBuffer(list);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ME substring(int start, int end) {
		if (strings == null) return (ME)this;
		int pos = 0;
		LinkedList<T> list = new LinkedList<>();
		for (int i = 0; i <= lastUsed; ++i) {
			int l = strings[i].length();
			if (start < pos + l) {
				int j = end - pos;
				if (j > l) j = l;
				list.add((T)strings[i].substring(start - pos, j));
				start = pos + j;
				if (start >= end) break;
			}
			pos += l;
		}
		return createBuffer(list);
	}
	
	/** Compare this String with another. */
	@SuppressWarnings("squid:S1201") // we want the name equals
	public boolean equals(ME s) {
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
	public int fillIso8859Bytes(byte[] bytes, int start) {
		if (strings == null) return 0;
		int pos = 0;
		for (int i = 0; i <= lastUsed; ++i)
			pos += strings[i].fillIso8859Bytes(bytes, start + pos);
		return pos;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME trimBeginning() {
		if (strings != null)
			strings[0].trimBeginning();
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME trimEnd() {
		if (strings != null)
			strings[lastUsed].trimEnd();
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME removeStartChars(int nb) {
		while (strings != null) {
			int l = strings[0].length();
			if (nb < l) {
				strings[0].removeStartChars(nb);
				return (ME)this;
			}
			if (lastUsed == 0) {
				strings = null;
				return (ME)this;
			}
			System.arraycopy(strings, 1, strings, 0, lastUsed);
			strings[lastUsed] = null;
			lastUsed--;
			nb -= l;
		}
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME removeEndChars(int nb) {
		while (strings != null) {
			int l = strings[lastUsed].length();
			if (nb < l) {
				strings[lastUsed].removeEndChars(nb);
				return (ME)this;
			}
			if (lastUsed == 0) {
				strings = null;
				return (ME)this;
			}
			strings[lastUsed] = null;
			lastUsed--;
			nb -= l;
		}
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public LinkedList<ME> split(char sep) {
		LinkedList<ME> list = new LinkedList<>();
		if (strings == null) return list;
		int index = 0;
		ME current = createBuffer();
		while (index <= lastUsed) {
			int pos = 0;
			do {
				int i = strings[index].indexOf(sep, pos);
				if (i < 0) {
					current.append(strings[index].substring(pos, strings[index].length()));
					break;
				}
				if (current.isEmpty()) {
					list.add(createBuffer((T)strings[index].substring(pos, i)));
				} else {
					current.append(strings[index].substring(pos, i));
					list.add(current);
					current = createBuffer();
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
	@SuppressWarnings("unchecked")
	private ME subBuffer(int startBuffer, int startBufferIndex, int endBuffer, int endBufferIndex) {
		ME result = createBuffer();
		result.strings = allocateArray(endBuffer - startBuffer + 1);
		result.lastUsed = result.strings.length - 1;
		for (int buffer = startBuffer; buffer <= endBuffer; ++buffer) {
			int start = buffer == startBuffer ? startBufferIndex : 0;
			int end = buffer == endBuffer ? endBufferIndex : strings[buffer].length();
			result.strings[buffer - startBuffer] = (T)strings[buffer].substring(start, end);
		}
		return result;
	}
	
	@SuppressWarnings({ "squid:S3012", "unchecked" }) // false positive: Collections.addAll cannot be used on a subset of the array
	private void replace(int startBuffer, int startBufferIndex, int endBuffer, int endBufferIndex, ME replace) {
		ArrayList<T> list = new ArrayList<>(
			startBuffer + 1 + replace.lastUsed + 1 + lastUsed - endBuffer + 1
		);
		// add all strings before start
		for (int i = 0; i < startBuffer; ++i)
			list.add(strings[i]);
		// add start if necessary
		if (startBufferIndex > 0)
			list.add((T)strings[startBuffer].substring(0, startBufferIndex));
		// add replace
		if (replace.strings != null)
			for (int i = 0; i <= replace.lastUsed; ++i)
				list.add(replace.strings[i]);
		// add end if necessary
		if (endBufferIndex < strings[endBuffer].length() - 1)
			list.add((T)strings[endBuffer].substring(endBufferIndex + 1));
		// add all strings after end
		for (int i = endBuffer + 1; i <= lastUsed; ++i)
			list.add(strings[i]);
		
		strings = list.toArray(allocateArray(list.size()));
		lastUsed = strings.length - 1;
	}
	
	/** Replace all occurrences of search into replace. */
	@SuppressWarnings("unchecked")
	public ME replace(CharSequence search, T replace) {
		int pos = 0;
		while ((pos = indexOf(search, pos)) >= 0) {
			replace(pos, pos + search.length() - 1, replace);
			pos = pos + replace.length();
		}
		return (ME)this;
	}
	
	/** Replace all occurrences of search into replace. */
	@Override
	public ME replace(CharSequence search, CharSequence replace) {
		return replace(search, createString(replace));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME replace(char oldChar, char newChar) {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].replace(oldChar, newChar);
		return (ME)this;
	}
	
	/** Replace all occurrences of oldChar into replaceValue. */
	public ME replace(char oldChar, CharSequence replaceValue) {
		return replace(oldChar, createString(replaceValue));
	}

	/** Replace all occurrences of oldChar into replaceValue. */
	@SuppressWarnings("unchecked")
	public ME replace(char oldChar, T replaceValue) {
		if (replaceValue.length() == 1)
			return replace(oldChar, replaceValue.charAt(0));
		if (strings == null)
			return (ME)this;
		int pos = 0;
		while ((pos = indexOf(oldChar, pos)) >= 0) {
			replace(pos, pos, replaceValue);
			pos = pos + replaceValue.length();
		}
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME replace(int start, int end, CharSequence s) {
		if (s.getClass().equals(getArrayType()))
			replace(start, end, (T)s);
		else
			replace(start, end, createString(s));
		return (ME)this;
	}
	
	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	@SuppressWarnings("unchecked")
	public void replace(int start, int end, T s) {
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
			start == firstBufferPos ? null : (T)strings[firstBufferIndex].substring(0, start - firstBufferPos),
			// remaining part of the last buffer
			end == lastBufferPos + lastBufferLen - 1 ? null : (T)strings[lastBufferIndex].substring(end - lastBufferPos + 1),
			// to insert
			1,
			s
		);
	}

	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	@SuppressWarnings("unchecked")
	public ME replace(int start, int end, ME s) {
		if (strings == null) return (ME)this;
		if (end < start) return (ME)this;
		int firstBufferIndex = 0;
		int firstBufferPos = 0;
		int firstBufferLen;
		do {
			firstBufferLen = strings[firstBufferIndex].length();
			if (start < firstBufferPos + firstBufferLen) break;
			firstBufferPos += firstBufferLen;
			if (++firstBufferIndex > lastUsed) return (ME)this;
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
			start == firstBufferPos ? null : (T)strings[firstBufferIndex].substring(0, start - firstBufferPos),
			// remaining part of the last buffer
			end == lastBufferPos + lastBufferLen - 1 ? null : (T)strings[lastBufferIndex].substring(end - lastBufferPos + 1),
			// to insert
			s.strings == null ? 0 : s.lastUsed + 1,
			s.strings
		);
		return (ME)this;
	}
	
	@SafeVarargs
	private final void replaceStrings(int startIndex, int endIndex,
		T first, T last, int nbMiddle, T... middle) {
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
		T[] list = allocateArray(nb + 3);
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
		CharSequence start, CharSequence end, UnaryOperator<ME> valueProvider
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
						ME variable = subBuffer(endOfStartBuffer, endOfStartBufferIndex, b, i);
						ME value = valueProvider.apply(variable);
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

	@SuppressWarnings("unchecked")
	@Override
	public ME toLowerCase() {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].toLowerCase();
		return (ME)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ME toUpperCase() {
		if (strings != null)
			for (int i = 0; i <= lastUsed; ++i)
				strings[i].toUpperCase();
		return (ME)this;
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
	
}
