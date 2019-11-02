package net.lecousin.framework.util;

import java.util.function.UnaryOperator;

/** Same as UnprotectedStringBuffer, but with a maximum size.
 * Every operation is accepted even the maximum size is reached, but the any new character won't be added.
 * Note that if an underlying UnprotectedString is modified, the limit may not be accurate.
 */
public class UnprotectedStringBufferLimited extends UnprotectedStringBuffer {

	/** Constructor. */
	public UnprotectedStringBufferLimited(int maxSize) {
		super();
		this.maxSize = maxSize;
	}
	
	protected int maxSize;
	protected int size;
	
	public int getMaxSize() {
		return maxSize;
	}
	
	@Override
	public UnprotectedStringBuffer append(char c) {
		if (size >= maxSize)
			return this;
		size++;
		return super.append(c);
	}
	
	@Override
	public UnprotectedStringBuffer append(char[] chars, int offset, int len) {
		if (size + len > maxSize) len = maxSize - size;
		if (len == 0)
			return this;
		size += len;
		return super.append(chars, offset, len);
	}
	
	@Override
	public UnprotectedStringBuffer append(CharSequence s) {
		if (size >= maxSize)
			return this;
		int l = s.length();
		if (size + l > maxSize) {
			l = maxSize - size;
			size = maxSize;
			return super.append(s.subSequence(0, l));
		}
		size += l;
		return super.append(s);
	}
	
	@Override
	public UnprotectedStringBuffer addFirst(UnprotectedString s) {
		if (size >= maxSize)
			return this;
		int l = s.length();
		if (size + l > maxSize) {
			l = maxSize - size;
			size = maxSize;
			return super.addFirst(s.substring(0, l));
		}
		size += l;
		return super.addFirst(s);
	}
	
	@Override
	public UnprotectedStringBuffer trimBeginning() {
		int l = length();
		super.trimBeginning();
		int l2 = length();
		size -= (l - l2);
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer trimEnd() {
		int l = length();
		super.trimEnd();
		int l2 = length();
		size -= (l - l2);
		return this;
	}
	
	@Override
	public UnprotectedStringBuffer removeStartChars(int nb) {
		if (nb > size) nb = size;
		if (nb == 0) return this;
		size -= nb;
		return super.removeStartChars(nb);
	}
	
	@Override
	public UnprotectedStringBuffer removeEndChars(int nb) {
		if (nb > size) nb = size;
		if (nb == 0) return this;
		size -= nb;
		return super.removeEndChars(nb);
	}
	
	@Override
	public void replace(int start, int end, UnprotectedString s) {
		int l = length();
		super.replace(start, end, s);
		int l2 = length();
		size -= (l - l2);
		if (size > maxSize)
			removeEndChars(size - maxSize);
	}
	
	@Override
	public UnprotectedStringBuffer replace(int start, int end, UnprotectedStringBuffer s) {
		int l = length();
		super.replace(start, end, s);
		int l2 = length();
		size -= (l - l2);
		if (size > maxSize)
			removeEndChars(size - maxSize);
		return this;
	}
	
	@Override
	public void searchAndReplace(
		CharSequence start, CharSequence end, UnaryOperator<UnprotectedStringBuffer> valueProvider
	) {
		int l = length();
		super.searchAndReplace(start, end, valueProvider);
		int l2 = length();
		size -= (l - l2);
		if (size > maxSize)
			removeEndChars(size - maxSize);
	}
}
