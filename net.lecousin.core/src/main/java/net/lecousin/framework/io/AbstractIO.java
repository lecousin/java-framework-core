package net.lecousin.framework.io;

import java.io.IOException;

import net.lecousin.framework.util.ConcurrentCloseable;

/** Utility class as a base for IO implementations. */
public abstract class AbstractIO extends ConcurrentCloseable<IOException> implements IO {

	/** Constructor. */
	public AbstractIO(String description, byte priority) {
		this.description = description;
		this.priority = priority;
	}
	
	protected String description;
	protected byte priority;
	
	@Override
	public String getSourceDescription() {
		return description;
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public void setPriority(byte priority) {
		this.priority = priority;
	}

}
