package net.lecousin.framework.io;

import java.io.IOException;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Utility class as a base for IO implementations. */
public abstract class AbstractIO extends ConcurrentCloseable<IOException> implements IO {

	/** Constructor. */
	public AbstractIO(String description, Priority priority) {
		this.description = description;
		this.priority = priority;
	}
	
	protected String description;
	protected Priority priority;
	
	@Override
	public String getSourceDescription() {
		return description;
	}

	@Override
	public Priority getPriority() {
		return priority;
	}

	@Override
	public void setPriority(Priority priority) {
		this.priority = priority;
	}

}
