package net.lecousin.framework.geometry;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Path in 2-dimension.
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class Path2D {

	/** Constructor. */
	public Path2D() {}
	
	/** Constructor. */
	public Path2D(Segment... segments) {
		for (Segment s : segments) this.segments.add(s);
	}
	
	private ArrayList<Segment> segments = new ArrayList<>();
	
	public List<Segment> getSegments() { return segments; }
	
	public void add(Segment s) { segments.add(s); }
	
	public void removeLastSegment() { segments.remove(segments.size() - 1); }
	
	/** Interface for a path segment. */
	public static interface Segment {}
	
	/** Segment to move to a new point. */
	public static class MoveTo implements Segment {
		/** Constructor. */
		public MoveTo(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public double x;
		public double y;
	}
	
	/** Line to a new point. */
	public static class LineTo implements Segment {
		/** Constructor. */
		public LineTo(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public double x;
		public double y;
	}
	
	/** Cubic bezier curve. */
	public static class CubicTo implements Segment {
		/** Constructor. */
		public CubicTo(double cx1, double cy1, double cx2, double cy2, double x, double y) {
			this.cx1 = cx1;
			this.cy1 = cy1;
			this.cx2 = cx2;
			this.cy2 = cy2;
			this.x = x;
			this.y = y;
		}
		
		public double cx1;
		public double cy1;
		public double cx2;
		public double cy2;
		public double x;
		public double y;
	}
	
	/** Quadratic curve. */
	public static class QuadTo implements Segment {
		/** Constructor. */
		public QuadTo(double cx, double cy, double x, double y) {
			this.cx = cx;
			this.cy = cy;
			this.x = x;
			this.y = y;
		}
		
		public double cx;
		public double cy;
		public double x;
		public double y;
	}
	
	/** Close the path. */
	public static class Close implements Segment {}
	
}
