package net.lecousin.framework.io.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Composite Chars (multiple Chars as a single one).
 * @param <T> type of Chars it contains
 */
public abstract class AbstractComposite<T extends DataBuffer> implements DataBuffer {

	/** Constructor. */
	public AbstractComposite() {
		list = new ArrayList<>();
		init();
	}
	
	/** Constructor. */
	@SafeVarargs
	public AbstractComposite(T... elements) {
		list = new ArrayList<>(elements.length);
		Collections.addAll(list, elements);
		init();
	}

	/** Constructor. */
	public AbstractComposite(List<T> elements) {
		list = new ArrayList<>(elements);
		init();
	}
	
	protected ArrayList<T> list;
	protected int position;
	protected int index;
	protected int length;
	
	private void init() {
		position = 0;
		index = 0;
		length = 0;
		for (T b : list) {
			b.setPosition(0);
			length += b.remaining();
		}
	}
	
	public List<T> getWrappedBuffers() {
		return list;
	}
	
	/** Append a new buffer to this composite. */
	public void add(T buffer) {
		list.add(buffer);
		buffer.setPosition(0);
		length += buffer.remaining();
	}
	
	@Override
	public int length() {
		return length;
	}
	
	@Override
	public int position() {
		return position;
	}
	
	@Override
	public void setPosition(int position) {
		if (position == this.position)
			return;
		if (position < this.position) {
			int toMove = this.position - position;
			if (index == list.size()) index--;
			do {
				T elements = list.get(index);
				int p = elements.position();
				if (toMove <= p) {
					elements.setPosition(p - toMove);
					this.position = position;
					return;
				}
				toMove -= p;
				index--;
				elements.setPosition(0);
			} while (true);
		}
		int toMove = position - this.position;
		do {
			T elements = list.get(index);
			int r = elements.remaining();
			if (toMove < r) {
				elements.moveForward(toMove);
				this.position = position;
				return;
			}
			elements.moveForward(r);
			toMove -= r;
			index++;
		} while (toMove > 0);
		this.position = position;
	}
	
	@Override
	public int remaining() {
		return length - position;
	}
	
	@Override
	public boolean hasRemaining() {
		return position < length;
	}
	
}
