package net.lecousin.framework.math;

import java.math.BigInteger;

/**
 * Range of integer values, with a minimum and maximum.
 */
public class RangeBigInteger {

	/** Constructor. */
	public RangeBigInteger(BigInteger min, BigInteger max) {
		this.min = min;
		this.max = max;
	}
	
	public BigInteger min;
	public BigInteger max;
	
	/** Create a copy of this instance. */
	public RangeBigInteger copy() {
		return new RangeBigInteger(min, max);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RangeBigInteger)) return false;
		RangeBigInteger r = (RangeBigInteger)obj;
		return r.min.equals(min) && r.max.equals(max);
	}

	@Override
	public int hashCode() {
		return min.intValue();
	}
	
	/** Return true if this range contains the given value. */
	public boolean contains(BigInteger value) {
		return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
	}
	
	/** Return the intersection between this range and the given range. */
	public RangeBigInteger intersect(RangeBigInteger o) {
		if (o.min.compareTo(min) < 0) {
			if (o.max.compareTo(min) < 0) return null; // o is before
			return new RangeBigInteger(min, o.max.compareTo(max) > 0 ? max : o.max);
		}
		if (max.compareTo(o.min) < 0) return null; // this is before
		return new RangeBigInteger(o.min, o.max.compareTo(max) > 0 ? max : o.max);
	}
	
	public BigInteger getLength() {
		return max.subtract(min).add(BigInteger.ONE);
	}
	
	@Override
	public String toString() {
		return "[" + min.toString() + "-" + max.toString() + "]";
	}
	
}
