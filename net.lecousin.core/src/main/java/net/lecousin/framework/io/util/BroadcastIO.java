package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Aggregation of several IOs, every write is performed on all IOs.
 */
public class BroadcastIO extends ConcurrentCloseable implements IO.Writable {

	/** Constructor. */
	public BroadcastIO(IO.Writable[] ios, byte priority, boolean closeIOs) {
		this.ios = ios;
		this.closeIOs = closeIOs;
		setPriority(priority);
	}
	
	private IO.Writable[] ios;
	private byte priority;
	private boolean closeIOs;
	
	@Override
	public byte getPriority() {
		return priority;
	}
	
	@Override
	public void setPriority(byte priority) {
		this.priority = priority;
		for (int i = 0; i < ios.length; ++i)
			ios[i].setPriority(priority);
	}
	
	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager(); // should we check if all IOs have the same ?
	}
	
	@Override
	public String getSourceDescription() {
		return "BroadcastIO"; // may be better aggregating description of IOs ?
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		if (!closeIOs) return new SynchronizationPoint<>(true);
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (int i = 0; i < ios.length; ++i)
			jp.addToJoin(ios[i].closeAsync());
		jp.start();
		return jp;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		ios = null;
		ondone.unblock();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		JoinPoint<IOException> jp = new JoinPoint<>();
		for (int i = 0; i < ios.length; ++i)
			jp.addToJoin(ios[i].canStartWriting());
		jp.start();
		return jp;
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		JoinPoint<IOException> jp = new JoinPoint<>();
		jp.addToJoin(ios.length);
		for (int i = 0; i < ios.length; ++i) {
			ios[i].writeAsync(buffer.duplicate()).listenInline(new AsyncWorkListener<Integer, IOException>() {
				
				@Override
				public void ready(Integer result) {
					if (result.intValue() != buffer.remaining())
						jp.error(new IOException("Only " + result.intValue()
							+ " byte(s) written instead of " + buffer.remaining()));
					else
						jp.joined();
				}
				
				@Override
				public void error(IOException error) {
					jp.error(error);
				}
				
				@Override
				public void cancelled(CancelException event) {
					jp.cancel(event);
				}
			});
		}
		jp.start();
		jp.blockException(0);
		return buffer.remaining();
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		new Task.Cpu.FromRunnable("BroadcastIO.writeAsync", priority, () -> {
			JoinPoint<IOException> jp = new JoinPoint<>();
			jp.addToJoin(ios.length);
			for (int i = 0; i < ios.length; ++i) {
				ios[i].writeAsync(buffer.duplicate()).listenInline(new AsyncWorkListener<Integer, IOException>() {
					
					@Override
					public void ready(Integer result) {
						if (result.intValue() != buffer.remaining())
							jp.error(new IOException("Only " + result.intValue()
								+ " byte(s) written instead of " + buffer.remaining()));
						else
							jp.joined();
					}
					
					@Override
					public void error(IOException error) {
						jp.error(error);
					}
					
					@Override
					public void cancelled(CancelException event) {
						jp.cancel(event);
					}
				});
			}
			jp.start();
			jp.listenInline(() -> {
				result.unblockSuccess(Integer.valueOf(buffer.remaining()));
			}, result);
		}).start();
		return result;
	}
	
}
