package net.lecousin.framework.application;

import java.util.ArrayList;

/**
 * A VersionRange is defined by a minimum version (may be null), and a maximum version (may be null).
 * The maximum version, if specified, may be included or excluded.
 */
public class VersionRange {

	/** Creates a VersionRange that includes a unique version number. */
	public VersionRange(Version unique) {
		min = unique.getNumbers();
		max = min;
		maxIncluded = true;
	}
	
	/** Creates a version range. */
	public VersionRange(Version min, Version max, boolean maxIncluded) {
		this.min = min != null ? min.getNumbers() : null;
		this.max = max != null ? max.getNumbers() : null;
		this.maxIncluded = maxIncluded;
	}
	
	private int[] min;
	private int[] max;
	private boolean maxIncluded;
	
	/** Return true if this VersionRange includes the given version. */
	public boolean includes(Version version) {
		return includes(version.getNumbers());
	}
	
	/** Return true if this VersionRange includes the given version. */
	public boolean includes(int[] v) {
		if (min != null && Version.compare(v, min) < 0) return false;
		if (max == null) return true;
		int c = Version.compare(v, max);
		if (c > 0) return false;
		if (c < 0) return true;
		return maxIncluded;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (min == max) Version.toString(min, s);
		else {
			s.append('[');
			Version.toString(min, s);
			if (max != null) {
				s.append('-');
				Version.toString(max, s);
				s.append(maxIncluded ? ']' : '[');
			}
		}
		return s.toString();
	}
	
	/** Parse a string into a list of version numbers (separated by a dot in the string).
	 * It stops at the first non numeric character which is not a dot. */
	public static int[] parse(String s) {
		ArrayList<Integer> n = new ArrayList<>();
		int val = 0;
		boolean hasChar = false;
		int i;
		for (i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				val = val * 10 + (c - '0');
				hasChar = true;
			} else if (c == '.') {
				n.add(Integer.valueOf(val));
				val = 0;
				hasChar = false;
			} else {
				if (hasChar)
					n.add(Integer.valueOf(val));
				break;
			}
		}
		if (i == s.length()) n.add(Integer.valueOf(val));
		int[] numbers = new int[n.size()];
		for (i = 0; i < n.size(); ++i)
			numbers[i] = n.get(i).intValue();
		return numbers;
	}
	
}
