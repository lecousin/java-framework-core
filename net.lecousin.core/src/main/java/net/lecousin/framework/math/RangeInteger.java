package net.lecousin.framework.math;

import java.text.ParseException;

import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.StringParser;
import net.lecousin.framework.util.StringParser.Parse;

/**
 * Range of integer values, with a minimum and maximum.
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RangeInteger {
	
	public int min;
	public int max;

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
	
	/** Parse from a String. */
	@Parse
	public RangeInteger(String string) throws ParseException {
		if (string == null || string.isEmpty())
			throw new ParseException("Empty string", 0);
		char c = string.charAt(0);
		if (c == ']' || c == '[') {
			int sep = string.indexOf('-');
			if (sep < 0)
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]", 1);
			try {
				min = Integer.parseInt(string.substring(1, sep));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 1);
			}
			if (c == ']') min++;
			c = string.charAt(string.length() - 1);
			if (c != ']' && c != '[')
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]",
					string.length() - 1);
			try {
				max = Integer.parseInt(string.substring(sep + 1, string.length() - 1));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), sep + 1);
			}
			if (c == '[') max--;
			if (max < min) {
				int i = min;
				min = max;
				max = i;
			}
		} else {
			try {
				min = max = Integer.parseInt(string);
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 0);
			}
		}
	}
	
	public RangeInteger copy() { return new RangeInteger(min,max); }
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RangeInteger)) return false;
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
	
	/** Remove the intersection between this range and the given range, and return the range before and the range after the intersection. */
	public Pair<RangeInteger,RangeInteger> removeIntersect(RangeInteger o) {
		if (o.max < min || o.min > max) // o is outside: no intersection
			return new Pair<>(copy(), null);
		if (o.min <= min) {
			// nothing before
			if (o.max >= max)
				return new Pair<>(null, null); // o is fully overlapping this
			return new Pair<>(null, new RangeInteger(o.max + 1, max));
		}
		if (o.max >= max) {
			// nothing after
			return new Pair<>(new RangeInteger(min, o.min - 1), null);
		}
		// in the middle
		return new Pair<>(new RangeInteger(min, o.min - 1), new RangeInteger(o.max + 1, max));
	}
	
	public int getLength() {
		return max - min + 1;
	}
	
	@Override
	public String toString() {
		return "[" + min + "-" + max + "]";
	}
	
	/** Parser from String to RangeInteger. */
	public static class Parser implements StringParser<RangeInteger> {
		@Override
		public RangeInteger parse(String string) throws ParseException {
			return new RangeInteger(string);
		}
	}
	
}
