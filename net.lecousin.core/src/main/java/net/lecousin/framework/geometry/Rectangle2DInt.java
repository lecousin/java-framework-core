package net.lecousin.framework.geometry;

/** A rectangle in 2-dimension with coordinates as integers. */
public class Rectangle2DInt {

	/** Constructor. */
	public Rectangle2DInt(int x, int y, int width, int height) {
		this(new Point2DInt(x, y), new Point2DInt(width, height));
	}
	
	/** Constructor. */
	public Rectangle2DInt(Point2DInt position, Point2DInt size) {
		this.position = position;
		this.size = size;
	}

	/** Constructor. */
	public Rectangle2DInt(Rectangle2DInt copy) {
		this(new Point2DInt(copy.position), new Point2DInt(copy.size));
	}
	
	public Point2DInt position;
	public Point2DInt size;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Rectangle2DInt)) return false;
		Rectangle2DInt o = (Rectangle2DInt)obj;
		return position.equals(o.position) && size.equals(o.size); 
	}
	
	@Override
	public int hashCode() {
		return position.hashCode();
	}
	
	@Override
	public String toString() {
		return "[x:" + position.x + ",y:" + position.y + ",w:" + size.x + ",h:" + size.y + "]";
	}
	
	/** return true if the given point is contained within this rectangle. */
	public boolean contains(Point2DInt point) {
		return point.x >= position.x && point.x < position.x + size.x && point.y >= position.y && point.y < position.y + size.y;
	}
	
	/** return true if the given point is contained within this rectangle. */
	public boolean contains(int px, int py) {
		return px >= position.x && px < position.x + size.x && py >= position.y && py < position.y + size.y;
	}

	/** return true if the given point is contained within this rectangle. */
	public boolean contains(Point2DInt point, int tolerance) {
		return
			point.x + tolerance >= position.x && point.x - tolerance < position.x + size.x &&
			point.y + tolerance >= position.y && point.y - tolerance < position.y + size.y;
	}

	/** return true if this rectangle contains entirely the given rectangle r. */ 
	public boolean contains(Rectangle2DInt r) {
		return
			r.position.x >= position.x &&
			r.position.x + r.size.x <= position.x + size.x &&
			r.position.y >= position.y &&
			r.position.y + r.size.y <= position.y + size.y;
	}
	
	/** return true if this rectangle contains the given rectangle, or has an intersection with it. */
	public boolean hasCommonArea(Rectangle2DInt r) {
		// if r starts after, cannot intersect
		if (r.position.x > position.x + size.x) return false;
		if (r.position.y > position.y + size.y) return false;
		// if r ends before, cannot intersect
		if (r.position.x + r.size.x - 1 < position.x) return false;
		if (r.position.y + r.size.y - 1 < position.y) return false;
		return true;
	}

	/** return the intersection points between this rectangle and the given line: it may returns 0, 1 or 2 points. */
	public Point2DInt[] getIntersection(Line2DInt line) {
		return line.getIntersection(this);
	}
	
	/** return the rectangle corresponding to the intersection or null if there is no intersection. */
	public Rectangle2DInt getIntersection(Rectangle2DInt r) {
		if (contains(r)) return new Rectangle2DInt(r);
		if (r.contains(this)) return new Rectangle2DInt(this);
		Rectangle2DInt left;
		Rectangle2DInt right;
		if (position.x < r.position.x) {
			left = this;
			right = r;
		} else {
			left = r;
			right = this;
		}
		if (right.position.x > left.position.x + left.size.x) return null;
		Rectangle2DInt top;
		Rectangle2DInt bottom;
		if (position.y < r.position.y) {
			top = this;
			bottom = r;
		} else {
			top = r;
			bottom = this;
		}
		if (bottom.position.y > top.position.y + top.size.y) return null;
		Point2DInt pos = new Point2DInt(right.position.x, bottom.position.y);
		Point2DInt size = new Point2DInt(
			Math.min(left.position.x + left.size.x - right.position.x, right.size.x),
			Math.min(top.position.y + top.size.y - bottom.position.y, bottom.size.y)
		);
		return new Rectangle2DInt(pos, size);
	}
	
	/** return the intersection points between this rectangle and the given one or null if both are equals:
	 * it may return 0 to 2 points (1 point being the case where only one of their corner is common). */
	public Point2DInt[] getIntersectionPoints(Rectangle2DInt r) {
		if (r.equals(this)) return null;
		if (contains(r)) return new Point2DInt[0];
		if (r.contains(this)) return new Point2DInt[0];
		Rectangle2DInt left;
		Rectangle2DInt right;
		if (position.x < r.position.x) {
			left = this;
			right = r;
		} else {
			left = r;
			right = this;
		}
		if (right.position.x > left.position.x + left.size.x) return new Point2DInt[0];
		Rectangle2DInt top;
		Rectangle2DInt bottom;
		if (position.y < r.position.y) {
			top = this;
			bottom = r;
		} else {
			top = r;
			bottom = this;
		}
		if (bottom.position.y > top.position.y + top.size.y) return new Point2DInt[0];
		if (left.position.x + left.size.x == right.position.x && top.position.y + top.size.y == bottom.position.y) 
			return new Point2DInt[] { new Point2DInt(left.position.x + left.size.x, top.position.y + top.size.y) };
		return new Point2DInt[] {
			new Point2DInt(right.position.x, top.position.y + top.size.y),
			new Point2DInt(left.position.x + left.size.x, bottom.position.y)
		};
	}
	
	/** Extand this rectangle so it can contain the given rectangle. */
	public void extendToContain(Rectangle2DInt r) {
		if (contains(r)) return;
		if (r.position.x < position.x) {
			size.x += position.x - r.position.x;
			position.x = r.position.x;
		}
		if (r.position.y < position.y) {
			size.y += position.y - r.position.y;
			position.y = r.position.y;
		}
		if (r.position.x + r.size.x > position.x + size.x) size.x = r.position.x + r.size.x - position.x;
		if (r.position.y + r.size.y > position.y + size.y) size.y = r.position.y + r.size.y - position.y;
	}
	
	/** Return the line of the top edge. */
	public Line2DInt getTopLine() {
		return new Line2DInt(
			new Point2DInt(position.x, position.y),
			new Point2DInt(position.x + size.x - 1, position.y)
		);
	}
	
	/** Return the line if the bottom edge. */
	public Line2DInt getBottomLine() {
		return new Line2DInt(
			new Point2DInt(position.x,position.y + size.y - 1),
			new Point2DInt(position.x + size.x - 1, position.y + size.y - 1)
		);
	}
	
	/** Return the line of the left edge. */
	public Line2DInt getLeftLine() {
		return new Line2DInt(
			new Point2DInt(position.x,position.y),
			new Point2DInt(position.x, position.y + size.y - 1)
		);
	}
	
	/** Return the line of the right edge. */
	public Line2DInt getRightLine() {
		return new Line2DInt(
			new Point2DInt(position.x + size.x - 1,position.y),
			new Point2DInt(position.x + size.x - 1, position.y + size.y - 1)
		);
	}

}
