package net.lecousin.framework.math;

import java.math.BigInteger;
import java.text.ParseException;

import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.StringParser;
import net.lecousin.framework.util.StringParser.Parse;

/**
 * Range of integer values, with a minimum and maximum.
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RangeBigInteger {
	
	public BigInteger min;
	public BigInteger max;

	/** Constructor. */
	public RangeBigInteger(BigInteger min, BigInteger max) {
		this.min = min;
		this.max = max;
	}
	
	/** Copy. */
	public RangeBigInteger(RangeBigInteger copy) {
		this.min = copy.min;
		this.max = copy.max;
	}
	
	/** Parse from a String. */
	@Parse
	public RangeBigInteger(String string) throws ParseException {
		if (string == null || string.isEmpty())
			throw new ParseException("Empty string", 0);
		char c = string.charAt(0);
		if (c == ']' || c == '[') {
			int sep = string.indexOf('-');
			if (sep < 0)
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]", 1);
			try {
				min = new BigInteger(string.substring(1, sep));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 1);
			}
			if (c == ']') min = min.add(BigInteger.ONE);
			c = string.charAt(string.length() - 1);
			if (c != ']' && c != '[')
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]",
					string.length() - 1);
			try {
				max = new BigInteger(string.substring(sep + 1, string.length() - 1));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number! " + e.getMessage(), sep + 1);
			}
			if (c == '[') max = max.subtract(BigInteger.ONE);
			if (max.compareTo(min) < 0) {
				BigInteger i = min;
				min = max;
				max = i;
			}
		} else {
			try {
				min = max = new BigInteger(string);
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 0);
			}
		}
	}
	
	public RangeBigInteger copy() { return new RangeBigInteger(min,max); }
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RangeBigInteger)) return false;
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
	public RangeBigInteger intersect(RangeBigInteger r) {
		if (min.compareTo(r.max) > 0) return null;
		if (max.compareTo(r.min) < 0) return null;
		return new RangeBigInteger(min.compareTo(r.min) <= 0 ? r.min : min, max.compareTo(r.max) <= 0 ? max : r.max);
	}
	
	/** Remove the intersection between this range and the given range, and return the range before and the range after the intersection. */
	public Pair<RangeBigInteger,RangeBigInteger> removeIntersect(RangeBigInteger o) {
		if (o.max.compareTo(min) < 0 || o.min.compareTo(max) > 0) // o is outside: no intersection
			return new Pair<>(copy(), null);
		if (o.min.compareTo(min) <= 0) {
			// nothing before
			if (o.max.compareTo(max) >= 0)
				return new Pair<>(null, null); // o is fully overlapping this
			return new Pair<>(null, new RangeBigInteger(o.max.add(BigInteger.ONE), max));
		}
		if (o.max.compareTo(max) >= 0) {
			// nothing after
			return new Pair<>(new RangeBigInteger(min, o.min.subtract(BigInteger.ONE)), null);
		}
		// in the middle
		return new Pair<>(new RangeBigInteger(min, o.min.subtract(BigInteger.ONE)), new RangeBigInteger(o.max.add(BigInteger.ONE), max));
	}
	
	public BigInteger getLength() {
		return max.subtract(min).add(BigInteger.ONE);
	}
	
	@Override
	public String toString() {
		return "[" + min.toString() + "-" + max.toString() + "]";
	}
	
	/** Parser from String to RangeInteger. */
	public static class Parser implements StringParser<RangeBigInteger> {
		@Override
		public RangeBigInteger parse(String string) throws ParseException {
			return new RangeBigInteger(string);
		}
	}

}
