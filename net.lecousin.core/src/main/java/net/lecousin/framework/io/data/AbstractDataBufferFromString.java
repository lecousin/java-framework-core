package net.lecousin.framework.io.data;

/** Chars Readable implementation wrapping a String. */
public abstract class AbstractDataBufferFromString implements DataBuffer {

	/** Constructor. */
	public AbstractDataBufferFromString(String str) {
		this(str, 0, str.length());
	}

	/** Constructor. */
	public AbstractDataBufferFromString(String str, int offset, int length) {
		this.str = str;
		this.offset = offset;
		this.length = length;
	}
	
	protected String str;
	protected int offset;
	protected int length;
	protected int pos = 0;

	@Override
	public int length() {
		return length;
	}

	@Override
	public int position() {
		return pos;
	}

	@Override
	public void setPosition(int position) {
		pos = position;
	}

	@Override
	public int remaining() {
		return length - pos;
	}

}
