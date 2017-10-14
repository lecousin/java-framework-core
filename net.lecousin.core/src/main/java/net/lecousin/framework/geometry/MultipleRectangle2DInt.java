package net.lecousin.framework.geometry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Several rectangles.
 */
public class MultipleRectangle2DInt {

	/** Constructor. */
	public MultipleRectangle2DInt() {
	}
	
	private LinkedList<Rectangle2DInt> rectangles = new LinkedList<>();
	
	/** Add a rectangle. */
	public void add(Rectangle2DInt rect) {
		for (Iterator<Rectangle2DInt> it = rectangles.iterator(); it.hasNext(); ) {
			Rectangle2DInt r = it.next();
			if (r.contains(rect)) {
				return;
			}
			if (rect.contains(r)) it.remove();
		}
		// TODO better
		rectangles.add(rect);
	}
	
	/** Add a rectangle. */
	public void add(int x, int y, int w, int h) {
		add(new Rectangle2DInt(x,y,w,h));
	}
	
	public List<Rectangle2DInt> getRectangles() {
		return rectangles;
	}
	
}
