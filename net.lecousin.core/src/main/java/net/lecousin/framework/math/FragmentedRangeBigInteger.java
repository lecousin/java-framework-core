package net.lecousin.framework.math;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.util.StringParser;
import net.lecousin.framework.util.StringParser.Parse;

/**
 * List of RangeBigInteger representing a fragmented data.
 */
public class FragmentedRangeBigInteger extends LinkedList<RangeBigInteger> {
	
	private static final long serialVersionUID = -2633315842445860994L;

	/** Constructor. */
	public FragmentedRangeBigInteger() {
		super();
	}

	/** Constructor. */
	public FragmentedRangeBigInteger(RangeBigInteger r) {
		super();
		add(r);
	}
	
	/** Parse from string. */
	@Parse
	public FragmentedRangeBigInteger(String string) throws ParseException, NumberFormatException {
		if (string == null || string.isEmpty())
			return;
		char c = string.charAt(0);
		if (c == '{') {
			int end = string.indexOf('}');
			if (end < 0)
				throw new ParseException("Missing }", 0);
			string = string.substring(1, end);
		}
		String[] ranges = string.split(",");
		for (String range : ranges) {
			range = range.trim();
			if (range.isEmpty()) continue;
			addRange(new RangeBigInteger(range));
		}
	}
	
	/** Return the intersection between the 2 fragmented data. */
	public static FragmentedRangeBigInteger intersect(FragmentedRangeBigInteger list1, FragmentedRangeBigInteger list2) {
		FragmentedRangeBigInteger result = new FragmentedRangeBigInteger();
		if (list1.isEmpty() || list2.isEmpty()) return result;
		for (RangeBigInteger r1 : list1) {
			for (RangeBigInteger r2 : list2) {
				if (r2.max.compareTo(r1.min) < 0) continue;
				if (r2.min.compareTo(r1.max) > 0) break;
				BigInteger min = r1.min.max(r2.min);
				BigInteger max = r1.max.min(r2.max);
				result.addRange(min, max);
			}
		}
		return result;
	}
	
	/** Create a copy of this instance. */
	public FragmentedRangeBigInteger copy() {
		FragmentedRangeBigInteger c = new FragmentedRangeBigInteger();
		for (RangeBigInteger r : this) c.add(new RangeBigInteger(r));
		return c;
	}
	
	public void addRange(RangeBigInteger r) { addRange(r.min, r.max); }
	
	/** Add the given range. */
	public void addRange(BigInteger start, BigInteger end) {
		if (isEmpty()) {
			add(new RangeBigInteger(start, end));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeBigInteger r = get(i);
			if (end.compareTo(r.min) < 0) { 
				if (end.equals(r.min.subtract(BigInteger.ONE)))
					r.min = start;
				else
					add(i, new RangeBigInteger(start, end)); 
				return; 
			}
			if (start.equals(r.max.add(BigInteger.ONE))) {
				r.max = end;
				for (int j = i + 1; j < size(); ) {
					RangeBigInteger r2 = get(j);
					int c = end.compareTo(r2.min.subtract(BigInteger.ONE));
					if (c < 0) break;
					if (c == 0) {
						r.max = r2.max;
						remove(j);
						break;
					}
					if (end.compareTo(r2.max) >= 0) {
						remove(j);
						continue;
					}
					r.max = r2.max;
					remove(j);
					break;
				}
				return;
			}
			if (start.compareTo(r.max) > 0) continue;
			if (start.compareTo(r.min) < 0) r.min = start;
			if (end.compareTo(r.max) <= 0) return;
			r.max = end;
			for (int j = i + 1; j < size(); ) {
				RangeBigInteger r2 = get(j);
				if (end.compareTo(r2.max) >= 0) {
					remove(j);
					continue;
				}
				if (end.compareTo(r2.min.subtract(BigInteger.ONE)) < 0) break;
				r.max = r2.max;
				remove(j);
				break;
			}
			return;
		}
		add(new RangeBigInteger(start, end));
	}
	
	/** Add the given ranges. */
	public void addRanges(Collection<RangeBigInteger> ranges) {
		for (RangeBigInteger r : ranges)
			addRange(r);
	}
	
	/** Add a single value. */
	public void addValue(BigInteger value) {
		if (isEmpty()) {
			add(new RangeBigInteger(value, value));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeBigInteger r = get(i);
			if (value.compareTo(r.min) < 0) { 
				if (value.compareTo(r.min.subtract(BigInteger.ONE)) == 0)
					r.min = value;
				else
					add(i, new RangeBigInteger(value, value)); 
				return; 
			}
			if (value.compareTo(r.max.add(BigInteger.ONE)) == 0) {
				r.max = value;
				if (i < size() - 1) {
					RangeBigInteger r2 = get(i + 1);
					if (r2.min.compareTo(value.add(BigInteger.ONE)) == 0) {
						r.max = r2.max;
						remove(i + 1);
					}
				}
				return;
			}
			if (value.compareTo(r.max) <= 0) return;
		}
		add(new RangeBigInteger(value, value));
	}
	
	/** Return true if this fragmented data contains the given offset. */
	public boolean containsValue(long val) {
		BigInteger b = BigInteger.valueOf(val);
		return containsValue(b);
	}
	
	/** Return true if this fragmented data contains the given offset. */
	public boolean containsValue(BigInteger val) {
		for (RangeBigInteger r : this) {
			int c = r.min.compareTo(val);
			if (c <= 0 && val.compareTo(r.max) <= 0) return true;
			if (c > 0) return false;
		}
		return false;
	}

	/** Return true if this fragmented data contains the given range of offset. */
	public boolean containsRange(BigInteger min, BigInteger max) {
		for (RangeBigInteger r : this) {
			int i = min.compareTo(r.min);
			if (i >= 0 && max.compareTo(r.max) <= 0) return true;
			if (i < 0) return false;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given range. */
	public boolean containsOneValueIn(RangeBigInteger range) {
		for (RangeBigInteger r : this) {
			if (r.max.compareTo(range.min) < 0) continue;
			if (r.min.compareTo(range.max) > 0) break;
			return true;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given ranges. */
	public boolean containsOneValueIn(Collection<RangeBigInteger> ranges) {
		for (RangeBigInteger r : ranges)
			if (containsOneValueIn(r))
				return true;
		return false;
	}

	/** Return the minimum value. */
	public BigInteger getMin() {
		if (isEmpty()) return BigInteger.ZERO;
		return getFirst().min;
	}

	/** Return the maximum value. */
	public BigInteger getMax() {
		if (isEmpty()) return BigInteger.ZERO;
		return getLast().max;
	}
	
	/**
	 * If a range with the exact size exists, it is returned.
	 * Else, the smaller range greater than the given size is returned.
	 * If no range can contain the size, null is returned.
	 */
	public RangeBigInteger removeBestRangeForSize(BigInteger size) {
		RangeBigInteger best = null;
		BigInteger bestSize = null;
		for (Iterator<RangeBigInteger> it = iterator(); it.hasNext(); ) {
			RangeBigInteger r = it.next();
			BigInteger l = r.getLength();
			int c = size.compareTo(l);
			if (c == 0) {
				it.remove();
				return r;
			}
			if (c > 0) continue;
			if (bestSize == null || bestSize.compareTo(l) > 0) {
				best = r;
				bestSize = l;
			}
		}
		if (best == null) return null;
		RangeBigInteger res = new RangeBigInteger(best.min, best.min.add(size).subtract(BigInteger.ONE));
		best.min = best.min.add(size);
		return res;
	}
	
	/** Remove the largest range. */
	public RangeBigInteger removeBiggestRange() {
		if (isEmpty()) return null;
		if (size() == 1) return remove(0);
		int biggestIndex = 0;
		RangeBigInteger r = get(0);
		BigInteger biggestSize = r.getLength();
		for (int i = 1; i < size(); ++i) {
			r = get(i);
			BigInteger l = r.getLength();
			if (l.compareTo(biggestSize) > 0) {
				biggestSize = l;
				biggestIndex = i;
			}
		}
		return remove(biggestIndex);
	}
	
	/** Remove and return the first value, or null if empty. */
	public BigInteger removeFirstValue() {
		if (isEmpty()) return null;
		RangeBigInteger r = getFirst();
		BigInteger value = r.min;
		if (r.min.equals(r.max)) removeFirst();
		else r.min = r.min.add(BigInteger.ONE);
		return value;
	}

	/** Remove the given range. */
	public void removeRange(BigInteger start, BigInteger end) {
		for (int i = 0; i < size(); ++i) {
			RangeBigInteger r = get(i);
			if (r.min.compareTo(end) > 0) return;
			if (r.max.compareTo(start) < 0) continue;
			if (r.min.compareTo(start) < 0) {
				if (r.max.equals(end)) {
					r.max = start.subtract(BigInteger.ONE);
					return;
				} else if (r.max.compareTo(end) < 0) {
					BigInteger j = r.max;
					r.max = start.subtract(BigInteger.ONE);
					start = j.add(BigInteger.ONE);
					continue;
				} else {
					RangeBigInteger nr = new RangeBigInteger(end.add(BigInteger.ONE), r.max);
					r.max = start.subtract(BigInteger.ONE);
					add(i + 1, nr);
					return;
				}
			} else if (r.min.equals(start)) {
				if (r.max.equals(end)) {
					remove(i);
					return;
				} else if (r.max.compareTo(end) < 0) {
					remove(i);
					start = r.max.add(BigInteger.ONE);
					i--;
					continue;
				} else {
					r.min = end.add(BigInteger.ONE);
					return;
				}
			} else {
				if (r.max.equals(end)) {
					remove(i);
					return;
				} else if (r.max.compareTo(end) < 0) {
					remove(i);
					start = r.max.add(BigInteger.ONE);
					i--;
					continue;
				} else {
					r.min = end.add(BigInteger.ONE);
					return;
				}
			}
		}
	}
	
	/** Remove a single offset. */
	public void removeValue(BigInteger value) {
		removeRange(value, value);
	}
	
	/** Return the total size, summing the ranges length. */
	public BigInteger getTotalSize() {
		BigInteger total = BigInteger.ZERO;
		for (RangeBigInteger r : this)
			total = total.add(r.getLength());
		return total;
	}

	/** Add the given ranges. */
	public void addCopy(Collection<RangeBigInteger> col) {
		for (RangeBigInteger r : col)
			addRange(r.min, r.max);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("{");
		boolean first = true;
		for (RangeBigInteger r : this) {
			if (first) first = false;
			else s.append(",");
			s.append("[").append(r.min).append("-").append(r.max).append("]");
		}
		s.append("}");
		return s.toString();
	}
	
	/** String parser. */
	public static class Parser implements StringParser<FragmentedRangeBigInteger> {
		@Override
		public FragmentedRangeBigInteger parse(String string) throws ParseException {
			return new FragmentedRangeBigInteger(string);
		}
	}
	
}
