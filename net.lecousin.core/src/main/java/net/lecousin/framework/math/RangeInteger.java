package net.lecousin.framework.math;

/**
 * Range of integer values, with a minimum and maximum.
 */
public class RangeInteger {

	/** Constructor. */
	public RangeInteger(int min, int max) {
		this.min = min;
		this.max = max;
	}

	/** Copy constructor. */
	public RangeInteger(RangeInteger copy) {
		this.min = copy.min;
		this.max = copy.max;
	}
	
	public int min;
	public int max;
	
	public RangeInteger copy() { return new RangeInteger(min,max); }
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RangeInteger)) return false;
		RangeInteger r = (RangeInteger)obj;
		return r.min == min && r.max == max;
	}

	@Override
	public int hashCode() {
		return min + max;
	}
	
	/** Return true if this range contains the given value. */
	public boolean contains(int value) {
		return value >= min && value <= max;
	}
	
	/** Return the intersection between this range and the given range. */
	public RangeInteger intersect(RangeInteger r) {
		if (min > r.max) return null;
		if (max < r.min) return null;
		return new RangeInteger(Math.max(min, r.min), Math.min(max, r.max));
	}
	
	public int getLength() {
		return max - min + 1;
	}
	
	@Override
	public String toString() {
		return "[" + min + "-" + max + "]";
	}
	
}
