package net.lecousin.framework.math;

import net.lecousin.framework.util.Pair;

/**
 * Range of long values, with a minimum and maximum.
 */
public class RangeLong {

	/** Constructor. */
	public RangeLong(long min, long max) {
		this.min = min;
		this.max = max;
	}
	
	/** Copy. */
	public RangeLong(RangeLong copy) {
		this.min = copy.min;
		this.max = copy.max;
	}
	
	public long min;
	public long max;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RangeLong)) return false;
		RangeLong r = (RangeLong)obj;
		return r.min == min && r.max == max;
	}

	@Override
	public int hashCode() {
		return (int)(min + max);
	}
	
	/** Return true if this range contains the given value. */
	public boolean contains(long value) {
		return value >= min && value <= max;
	}
	
	public long getLength() {
		return max - min + 1;
	}
	
	/** Return the intersection between this range and the given range. */
	public RangeLong intersect(RangeLong o) {
		if (o.min < min) {
			if (o.max < min) return null; // o is before
			return new RangeLong(min, o.max > max ? max : o.max);
		}
		if (max < o.min) return null; // this is before
		return new RangeLong(o.min, o.max > max ? max : o.max);
	}
	
	/** Remove the intersection between this range and the given range, and return the range before and the range after the intersection. */
	public Pair<RangeLong,RangeLong> removeIntersect(RangeLong o) {
		RangeLong before;
		RangeLong after;
		if (min < o.min) {
			if (max < o.min) {
				before = new RangeLong(this);
				after = null;
			} else {
				before = new RangeLong(min, o.min);
				if (o.max >= max)
					after = null;
				else
					after = new RangeLong(max + 1, o.max);
			}
		} else {
			before = null;
			if (max <= o.max)
				after = null;
			else
				after = new RangeLong(max + 1, o.max);
		}
		return new Pair<RangeLong,RangeLong>(before, after);
	}
	
	@Override
	public String toString() {
		return "[" + min + "-" + max + "]";
	}
	
}
