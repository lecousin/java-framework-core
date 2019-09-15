package net.lecousin.framework.application;

import java.util.Comparator;

/**
 * Specifies a version.
 */
public interface VersionSpecification extends Comparator<Version> {

	/** Return true if the given version is compatible with the specified version. */
	boolean isMatching(Version version);
	
	/** Only one specific version is accepted. */ 
	public static class SingleVersion implements VersionSpecification {
		
		/** Constructor. */
		public SingleVersion(Version version) {
			this.version = version;
		}
		
		private Version version;
		
		public Version getVersion() { return version; }
		
		@Override
		public boolean isMatching(Version version) {
			return version.compareTo(this.version) == 0;
		}
		
		@Override
		public int compare(Version o1, Version o2) {
			return 0; // only one can match
		}
		
		@Override
		public String toString() {
			return version.toString();
		}
	}
	
	/** A range of version is accepted. */
	public static class Range implements VersionSpecification {
		
		/** Constructor. */
		public Range(VersionRange range) {
			this.vrange = range;
		}
		
		private VersionRange vrange;
		
		public VersionRange getRange() { return vrange; }
		
		@Override
		public boolean isMatching(Version version) {
			return vrange.includes(version);
		}
		
		@Override
		public int compare(Version o1, Version o2) {
			return o1.compareTo(o2);
		}
	
		@Override
		public String toString() {
			return vrange.toString();
		}
	}
	
	/** A specific version is wished, but a range is accepted. */
	public static class RangeWithRecommended implements VersionSpecification {
		
		/** Constructor. */
		public RangeWithRecommended(VersionRange range, Version recommended) {
			this.range = range;
			this.recommended = recommended;
		}
		
		private VersionRange range;
		Version recommended;
		
		public Version getRecommended() { return recommended; }
		
		public VersionRange getRange() { return range; }
		
		@Override
		public boolean isMatching(Version version) {
			return range.includes(version);
		}
		
		@Override
		public int compare(Version o1, Version o2) {
			if (o1.compareTo(recommended) == 0) return 1;
			if (o2.compareTo(recommended) == 0) return -1;
			return o1.compareTo(o2);
		}
		
		@Override
		public String toString() {
			return recommended.toString() + " (or " + range.toString() + ")";
		}
		
	}
	
}
