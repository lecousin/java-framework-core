package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.AsyncSupplier.Listener;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Aggregation of several IOs, every write is performed on all IOs.
 */
public class BroadcastIO extends ConcurrentCloseable<IOException> implements IO.Writable {

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
	protected IAsync<IOException> closeUnderlyingResources() {
		if (!closeIOs) return new Async<>(true);
		JoinPoint<IOException> jp = new JoinPoint<>();
		for (int i = 0; i < ios.length; ++i)
			jp.addToJoin(ios[i].closeAsync());
		jp.start();
		return jp;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		ios = null;
		ondone.unblock();
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
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
		int nb = buffer.remaining();
		for (int i = 0; i < ios.length; ++i) {
			ios[i].writeAsync(
				i == ios.length - 1 ? buffer : buffer.duplicate()
			).listen(new Listener<Integer, IOException>() {
				
				@Override
				public void ready(Integer result) {
					if (result.intValue() != nb)
						jp.error(new IOException("Only " + result.intValue()
							+ " byte(s) written instead of " + nb));
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
		return nb;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		new Task.Cpu.FromRunnable("BroadcastIO.writeAsync", priority, () -> {
			JoinPoint<IOException> jp = new JoinPoint<>();
			jp.addToJoin(ios.length);
			int nb = buffer.remaining();
			for (int i = 0; i < ios.length; ++i) {
				ios[i].writeAsync(
					i == ios.length - 1 ? buffer : buffer.duplicate()
				).listen(new Listener<Integer, IOException>() {
					
					@Override
					public void ready(Integer result) {
						if (result.intValue() != nb)
							jp.error(new IOException("Only " + result.intValue()
								+ " byte(s) written instead of " + nb));
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
			jp.onDone(() -> result.unblockSuccess(Integer.valueOf(nb)), result);
		}).start();
		return result;
	}
	
}
