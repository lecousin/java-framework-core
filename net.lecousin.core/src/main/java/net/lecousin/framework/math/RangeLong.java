package net.lecousin.framework.math;

import java.text.ParseException;

import net.lecousin.framework.text.StringParser;
import net.lecousin.framework.text.StringParser.Parse;
import net.lecousin.framework.util.Pair;

/**
 * Range of long values, with a minimum and maximum.
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RangeLong {
	
	public long min;
	public long max;

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
	
	/** Parse from a String. */
	@Parse
	@SuppressWarnings("squid:S1192") // same string 3 times
	public RangeLong(String string) throws ParseException {
		if (string == null || string.isEmpty())
			throw new ParseException("Empty string", 0);
		char c = string.charAt(0);
		if (c == ']' || c == '[') {
			int sep = string.indexOf('-');
			if (sep < 0)
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]", 1);
			try {
				min = Long.parseLong(string.substring(1, sep));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 1);
			}
			if (c == ']') min++;
			c = string.charAt(string.length() - 1);
			if (c != ']' && c != '[')
				throw new ParseException("Must start with [ or ], followed by a number, a -, a number, and finally [ or ]",
					string.length() - 1);
			try {
				max = Long.parseLong(string.substring(sep + 1, string.length() - 1));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), sep + 1);
			}
			if (c == '[') max--;
			if (max < min) {
				long i = min;
				min = max;
				max = i;
			}
		} else {
			try {
				min = max = Long.parseLong(string);
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid number: " + e.getMessage(), 0);
			}
		}
	}

	public RangeLong copy() { return new RangeLong(min,max); }

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RangeLong)) return false;
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
	public RangeLong intersect(RangeLong r) {
		if (min > r.max) return null;
		if (max < r.min) return null;
		return new RangeLong(Math.max(min, r.min), Math.min(max, r.max));
	}
	
	/** Remove the intersection between this range and the given range, and return the range before and the range after the intersection. */
	public Pair<RangeLong,RangeLong> removeIntersect(RangeLong o) {
		if (o.max < min || o.min > max) // o is outside: no intersection
			return new Pair<>(copy(), null);
		if (o.min <= min) {
			// nothing before
			if (o.max >= max)
				return new Pair<>(null, null); // o is fully overlapping this
			return new Pair<>(null, new RangeLong(o.max + 1, max));
		}
		if (o.max >= max) {
			// nothing after
			return new Pair<>(new RangeLong(min, o.min - 1), null);
		}
		// in the middle
		return new Pair<>(new RangeLong(min, o.min - 1), new RangeLong(o.max + 1, max));
	}
	
	@Override
	public String toString() {
		return "[" + min + "-" + max + "]";
	}
	
	/** Parser from String to RangeInteger. */
	public static class Parser implements StringParser<RangeLong> {
		@Override
		public RangeLong parse(String string) throws ParseException {
			return new RangeLong(string);
		}
	}
	
}
