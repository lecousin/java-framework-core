package net.lecousin.framework.geometry;

/**
 * Rectangle region edges' size.
 */
public class EdgeSizeInt {

	/** Constructor. */
	public EdgeSizeInt(int left, int right, int top, int bottom) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}
	
	public int left;
	public int right;
	public int top;
	public int bottom;
	
	public int width() { return left + right; }
	
	public int height() { return top + bottom; }
	
	/** Return a rectangle from the given one, removing the edges. */
	public Rectangle2DInt inside(Rectangle2DInt r) {
		return new Rectangle2DInt(r.position.x + left, r.position.y + top, r.size.x - left - right, r.size.y - top - bottom);
	}
	
}
