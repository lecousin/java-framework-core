package net.lecousin.framework.geometry;

/** A point in 2-dimension, with x and y as integers. */
public class Point2DInt {

	/** Constructor. */
	public Point2DInt(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/** Constructor. */
	public Point2DInt(Point2DInt copy) {
		this.x = copy.x;
		this.y = copy.y;
	}
	
	public int x;
	public int y;
	
	public int getX() { return x; }
	
	public int getY() { return y; }
	
	/** Return the distance between this point and the given one. */
	public double getDistance(Point2DInt c) {
		double xl = x > c.x ? x - c.x : c.x - x;
		double yl = y > c.y ? y - c.y : c.y - y;
		return Math.hypot(xl, yl);
	}
	
	/** Return a new point being this point with the given translation. */
	public Point2DInt translate(Point2DInt translation) {
		return new Point2DInt(x + translation.x, y + translation.y);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Point2DInt)) return false;
		Point2DInt c = (Point2DInt)obj;
		return x == c.x && y == c.y;
	}
	
	@Override
	public int hashCode() {
		return x + y;
	}
	
	@Override
	public String toString() {
		return "[" + x + "," + y + "]";
	}
}
