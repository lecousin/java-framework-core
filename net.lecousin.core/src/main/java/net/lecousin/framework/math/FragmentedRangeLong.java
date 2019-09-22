package net.lecousin.framework.math;

import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.lecousin.framework.util.StringParser;
import net.lecousin.framework.util.StringParser.Parse;

/**
 * List of RangeLong representing a fragmented data.
 */
public class FragmentedRangeLong extends LinkedList<RangeLong> {
	
	private static final long serialVersionUID = -2633315842445860994L;

	/** Constructor. */
	public FragmentedRangeLong() {
		super();
	}

	/** Constructor. */
	public FragmentedRangeLong(RangeLong r) {
		super();
		add(r);
	}
	
	/** Parse from string. */
	@Parse
	public FragmentedRangeLong(String string) throws ParseException {
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
			addRange(new RangeLong(range));
		}
	}

	/** Return the intersection between the 2 fragmented data. */
	public static FragmentedRangeLong intersect(FragmentedRangeLong list1, FragmentedRangeLong list2) {
		FragmentedRangeLong result = new FragmentedRangeLong();
		if (list1.isEmpty() || list2.isEmpty()) return result;
		for (RangeLong r1 : list1) {
			for (RangeLong r2 : list2) {
				if (r2.max < r1.min) continue;
				if (r2.min > r1.max) break;
				long min = r1.min < r2.min ? r2.min : r1.min;
				long max = r1.max > r2.max ? r2.max : r1.max;
				result.addRange(min, max);
			}
		}
		return result;
	}
	
	/** Create a copy of this instance. */
	public FragmentedRangeLong copy() {
		FragmentedRangeLong c = new FragmentedRangeLong();
		for (RangeLong r : this) c.add(new RangeLong(r));
		return c;
	}
	
	public void addRange(RangeLong r) { addRange(r.min, r.max); }
	
	/** Add the given range. */
	@SuppressWarnings("squid:S3776") // complexity
	public void addRange(long start, long end) {
		if (isEmpty()) {
			add(new RangeLong(start, end));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeLong r = get(i);
			if (end < r.min) { 
				if (end == r.min - 1)
					r.min = start;
				else
					add(i, new RangeLong(start, end)); 
				return; 
			}
			if (start == r.max + 1) {
				r.max = end;
				for (int j = i + 1; j < size(); ) {
					RangeLong r2 = get(j);
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
				RangeLong r2 = get(j);
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
		add(new RangeLong(start, end));
	}

	/** Add the given ranges. */
	public void addRanges(Collection<RangeLong> ranges) {
		for (RangeLong r : ranges)
			addRange(r);
	}

	/** Add a single value. */
	public void addValue(long value) {
		if (isEmpty()) {
			add(new RangeLong(value, value));
			return;
		}
		for (int i = 0; i < size(); ++i) {
			RangeLong r = get(i);
			if (value < r.min) { 
				if (value == r.min - 1)
					r.min = value;
				else
					add(i, new RangeLong(value, value)); 
				return; 
			}
			if (value == r.max + 1) {
				r.max = value;
				if (i < size() - 1) {
					RangeLong r2 = get(i + 1);
					if (r2.min == value + 1) {
						r.max = r2.max;
						remove(i + 1);
					}
				}
				return;
			}
			if (value <= r.max) return;
		}
		add(new RangeLong(value, value));
	}
	
	/** Return true if this fragmented data contains the given offset. */
	public boolean containsValue(long val) {
		for (RangeLong r : this) {
			if (val >= r.min && val <= r.max) return true;
			if (val < r.min) return false;
		}
		return false;
	}

	/** Return true if this fragmented data contains the given range of offset. */
	public boolean containsRange(long min, long max) {
		for (RangeLong r : this) {
			if (min >= r.min && max <= r.max) return true;
			if (min < r.min) return false;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given range. */
	public boolean containsOneValueIn(RangeLong range) {
		for (RangeLong r : this) {
			if (r.max < range.min) continue;
			if (r.min > range.max) break;
			return true;
		}
		return false;
	}
	
	/** Return true if this fragmented range contains at least one value of the given ranges. */
	public boolean containsOneValueIn(Collection<RangeLong> ranges) {
		for (RangeLong r : ranges)
			if (containsOneValueIn(r))
				return true;
		return false;
	}
	
	/** Return the minimum value. */
	public long getMin() {
		if (isEmpty()) return Long.MAX_VALUE;
		return getFirst().min;
	}

	/** Return the maximum value. */
	public long getMax() {
		if (isEmpty()) return Long.MIN_VALUE;
		return getLast().max;
	}
	
	/**
	 * If a range with the exact size exists, it is returned.
	 * Else, the smaller range greater than the given size is returned.
	 * If no range can contain the size, null is returned.
	 */
	public RangeLong removeBestRangeForSize(long size) {
		RangeLong best = null;
		long bestSize = Long.MAX_VALUE;
		for (Iterator<RangeLong> it = iterator(); it.hasNext(); ) {
			RangeLong r = it.next();
			if (r.max - r.min + 1 == size) {
				it.remove();
				return r;
			}
			if (r.max - r.min + 1 < size) continue;
			long s = r.max - r.min + 1;
			if (s < bestSize) {
				best = r;
				bestSize = s;
			}
		}
		if (best == null) return null;
		RangeLong res = new RangeLong(best.min, best.min + size - 1);
		best.min += size;
		return res;
	}
	
	/** Remove the largest range. */
	public RangeLong removeBiggestRange() {
		if (isEmpty()) return null;
		if (size() == 1) return remove(0);
		int biggestIndex = 0;
		RangeLong r = get(0);
		long biggestSize = r.max - r.min + 1;
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
	public Long removeFirstValue() {
		if (isEmpty()) return null;
		RangeLong r = getFirst();
		long value = r.min;
		if (r.min == r.max) removeFirst();
		else r.min++;
		return Long.valueOf(value);
	}
	
	/** Remove the given range. */
	@SuppressWarnings("squid:ForLoopCounterChangedCheck") // when removing an element, we need to change it
	public void removeRange(long start, long end) {
		for (int i = 0; i < size(); ++i) {
			RangeLong r = get(i);
			if (r.min > end) return;
			if (r.max < start) continue;
			if (r.min < start) {
				if (r.max == end) {
					r.max = start - 1;
					return;
				} else if (r.max < end) {
					long j = r.max;
					r.max = start - 1;
					start = j + 1;
				} else {
					RangeLong nr = new RangeLong(end + 1, r.max);
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
	public void removeValue(long value) {
		removeRange(value, value);
	}
	
	/** Return the total size, summing the ranges length. */
	public long getTotalSize() {
		long total = 0;
		for (RangeLong r : this)
			total += r.max - r.min + 1;
		return total;
	}
	
	/** Remove a value by index. */
	public long removeValueAt(long index) {
		for (RangeLong r : this) {
			long nb = r.max - r.min + 1;
			if (index >= nb) {
				index -= nb;
				continue;
			}
			long value = r.min + index;
			removeValue(value);
			return value;
		}
		throw new NoSuchElementException();
	}
	
	/** Add the given ranges. */
	public void addCopy(Collection<RangeLong> col) {
		for (RangeLong r : col)
			addRange(r.min, r.max);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("{");
		boolean first = true;
		for (RangeLong r : this) {
			if (first) first = false;
			else s.append(",");
			s.append("[").append(r.min).append("-").append(r.max).append("]");
		}
		s.append("}");
		return s.toString();
	}
	
	/** String parser. */
	public static class Parser implements StringParser<FragmentedRangeLong> {
		@Override
		public FragmentedRangeLong parse(String string) throws ParseException {
			return new FragmentedRangeLong(string);
		}
	}
	
}
