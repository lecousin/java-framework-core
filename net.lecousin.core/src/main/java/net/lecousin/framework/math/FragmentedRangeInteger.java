package net.lecousin.framework.math;

import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.util.StringParser;
import net.lecousin.framework.util.StringParser.Parse;

/**
 * List of RangeInteger representing a fragmented data.
 */
public class FragmentedRangeInteger extends LinkedList<RangeInteger> {
	
	private static final long serialVersionUID = -2633315842445860994L;

	/** Constructor. */
	public FragmentedRangeInteger() {
		super();
	}

	/** Constructor. */
	public FragmentedRangeInteger(RangeInteger r) {
		super();
		add(r);
	}
	
	/** Parse from string. */
	@Parse
	public FragmentedRangeInteger(String string) throws ParseException {
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
			addRange(new RangeInteger(range));
		}
	}
	
	/** Return the intersection between the 2 fragmented data. */
	public static FragmentedRangeInteger intersect(FragmentedRangeInteger list1, FragmentedRangeInteger list2) {
		FragmentedRangeInteger result = new FragmentedRangeInteger();
		if (list1.isEmpty() || list2.isEmpty()) return result;
		for (RangeInteger r1 : list1) {
			for (RangeInteger r2 : list2) {
				if (r2.max < r1.min) continue;
				if (r2.min > r1.max) break;
				int min = r1.min < r2.min ? r2.min : r1.min;
				int max = r1.max > r2.max ? r2.max : r1.max;
				result.addRange(min, max);
			}
		}
		return result;
	}
	
	/** Create a copy of this instance. */
	public FragmentedRangeInteger copy() {
		FragmentedRangeInteger c = new FragmentedRangeInteger();
		for (RangeInteger r : this) c.add(r.copy());
		return c;
	}
	
	public void addRange(RangeInteger r) { addRange(r.min, r.max); }
	
	/** Add the given range. */
	public void addRange(int start, int end) {
		if (isEmpty()) {
			add(new RangeInteger(start, end));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeInteger r = get(i);
			if (end < r.min) { 
				if (end == r.min - 1)
					r.min = start;
				else
					add(i, new RangeInteger(start, end)); 
				return; 
			}
			if (start == r.max + 1) {
				r.max = end;
				for (int j = i + 1; j < size(); ) {
					RangeInteger r2 = get(j);
					if (end < r2.min - 1) break;
					if (end == r2.min - 1) {
						r.max = r2.max;
						remove(j);
						break;
					}
					if (end >= r2.max) {
						remove(j);
						continue;
					}
					r.max = r2.max;
					remove(j);
					break;
				}
				return;
			}
			if (start > r.max) continue;
			if (start < r.min) r.min = start;
			if (end <= r.max) return;
			r.max = end;
			for (int j = i + 1; j < size(); ) {
				RangeInteger r2 = get(j);
				if (end >= r2.max) {
					remove(j);
					continue;
				}
				if (end < r2.min - 1) break;
				r.max = r2.max;
				remove(j);
				break;
			}
			return;
		}
		add(new RangeInteger(start, end));
	}

	/** Add the given ranges. */
	public void addRanges(Collection<RangeInteger> ranges) {
		for (RangeInteger r : ranges)
			addRange(r);
	}
	

	/** Add a single value. */
	public void addValue(int value) {
		if (isEmpty()) {
			add(new RangeInteger(value, value));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeInteger r = get(i);
			if (value < r.min) { 
				if (value == r.min - 1)
					r.min = value;
				else
					add(i, new RangeInteger(value, value)); 
				return; 
			}
			if (value == r.max + 1) {
				r.max = value;
				if (i < size() - 1) {
					RangeInteger r2 = get(i + 1);
					if (r2.min == value + 1) {
						r.max = r2.max;
						remove(i + 1);
					}
				}
				return;
			}
			if (value <= r.max) return;
		}
		add(new RangeInteger(value, value));
	}
	
	/** Return true if this fragmented data contains the given offset. */
	public boolean containsValue(int val) {
		for (RangeInteger r : this) {
			if (val >= r.min && val <= r.max) return true;
			if (val < r.min) return false;
		}
		return false;
	}

	/** Return true if this fragmented data contains the given range of offset. */
	public boolean containsRange(int start, int end) {
		if (start > end) return true;
		for (RangeInteger r : this) {
			if (r.min > start) return false;
			if (r.max < start) continue;
			return r.max >= end;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given range. */
	public boolean containsOneValueIn(RangeInteger range) {
		for (RangeInteger r : this) {
			if (r.max < range.min) continue;
			if (r.min > range.max) break;
			return true;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given ranges. */
	public boolean containsOneValueIn(Collection<RangeInteger> ranges) {
		for (RangeInteger r : ranges)
			if (containsOneValueIn(r))
				return true;
		return false;
	}
	
	/** Return the minimum value. */
	public int getMin() {
		if (isEmpty()) return Integer.MAX_VALUE;
		return getFirst().min;
	}

	/** Return the maximum value. */
	public int getMax() {
		if (isEmpty()) return Integer.MIN_VALUE;
		return getLast().max;
	}
	
	/**
	 * If a range with the exact size exists, it is returned.
	 * Else, the smaller range greater than the given size is returned.
	 * If no range can contain the size, null is returned.
	 */
	public RangeInteger removeBestRangeForSize(int size) {
		RangeInteger best = null;
		int bestSize = Integer.MAX_VALUE;
		for (Iterator<RangeInteger> it = iterator(); it.hasNext(); ) {
			RangeInteger r = it.next();
			if (r.max - r.min + 1 == size) {
				it.remove();
				return r;
			}
			if (r.max - r.min + 1 < size) continue;
			int s = r.max - r.min + 1;
			if (s < bestSize) {
				best = r;
				bestSize = s;
			}
		}
		if (best == null) return null;
		RangeInteger res = new RangeInteger(best.min, best.min + size - 1);
		best.min += size;
		return res;
	}
	
	/** Remove the largest range. */
	public RangeInteger removeBiggestRange() {
		if (isEmpty()) return null;
		if (size() == 1) return remove(0);
		int biggestIndex = 0;
		RangeInteger r = get(0);
		int biggestSize = r.max - r.min + 1;
		for (int i = 1; i < size(); ++i) {
			r = get(i);
			if (r.max - r.min + 1 > biggestSize) {
				biggestSize = r.max - r.min + 1;
				biggestIndex = i;
			}
		}
		return remove(biggestIndex);
	}
	
	/** Remove and return the first value, or null if empty. */
	public Integer removeFirstValue() {
		if (isEmpty()) return null;
		RangeInteger r = getFirst();
		int value = r.min;
		if (r.min == r.max) removeFirst();
		else r.min++;
		return Integer.valueOf(value);
	}

	/** Remove the given range. */
	@SuppressWarnings("squid:ForLoopCounterChangedCheck") // when removing an element, we need to change it
	public void remove(int start, int end) {
		for (int i = 0; i < size(); ++i) {
			RangeInteger r = get(i);
			if (r.min > end) return;
			if (r.max < start) continue;
			if (r.min < start) {
				if (r.max == end) {
					r.max = start - 1;
					return;
				} else if (r.max < end) {
					int j = r.max;
					r.max = start - 1;
					start = j + 1;
				} else {
					RangeInteger nr = new RangeInteger(end + 1, r.max);
					r.max = start - 1;
					add(i + 1, nr);
					return;
				}
			} else if (r.min == start) {
				if (r.max == end) {
					remove(i);
					return;
				} else if (r.max < end) {
					remove(i);
					start = r.max + 1;
					i--;
				} else {
					r.min = end + 1;
					return;
				}
			} else {
				if (r.max == end) {
					remove(i);
					return;
				} else if (r.max < end) {
					remove(i);
					start = r.max + 1;
					i--;
				} else {
					r.min = end + 1;
					return;
				}
			}
		}
	}
	
	/** Remove a single offset. */
	public void removeValue(int value) {
		remove(value, value);
	}
	
	/** Return the total size, summing the ranges length. */
	public int getTotalSize() {
		int total = 0;
		for (RangeInteger r : this)
			total += r.max - r.min + 1;
		return total;
	}
	
	/** Add the given ranges. */
	public void addCopy(Collection<RangeInteger> col) {
		for (RangeInteger r : col)
			addRange(r.min, r.max);
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("{");
		boolean first = true;
		for (RangeInteger r : this) {
			if (first) first = false;
			else s.append(",");
			s.append("[").append(r.min).append("-").append(r.max).append("]");
		}
		s.append("}");
		return s.toString();
	}
	
	/** String parser. */
	public static class Parser implements StringParser<FragmentedRangeInteger> {
		@Override
		public FragmentedRangeInteger parse(String string) throws ParseException {
			return new FragmentedRangeInteger(string);
		}
	}
	
}
