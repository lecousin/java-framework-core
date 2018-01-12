package net.lecousin.framework.math;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;

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
		for (RangeBigInteger r : this) c.add(r.copy());
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
	
}
