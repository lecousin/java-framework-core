package net.lecousin.framework.application;

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
	
}
