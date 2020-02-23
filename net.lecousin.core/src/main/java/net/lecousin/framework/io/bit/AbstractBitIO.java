package net.lecousin.framework.io.bit;

import java.io.IOException;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.AbstractIO;
import net.lecousin.framework.io.IO;

/** Base class for BitIO implementations.
 * @param <T> type of wrapped IO
 */
public abstract class AbstractBitIO<T extends IO> extends AbstractIO implements BitIO {

	protected AbstractBitIO(T io) {
		super(io.getSourceDescription(), io.getPriority());
		this.io = io;
	}
	
	protected T io;
	
	@Override
	public IO getWrappedIO() {
		return io;
	}
	
	@Override
	public void setPriority(Priority priority) {
		super.setPriority(priority);
		io.setPriority(priority);
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		ondone.unblock();
	}
	
}
