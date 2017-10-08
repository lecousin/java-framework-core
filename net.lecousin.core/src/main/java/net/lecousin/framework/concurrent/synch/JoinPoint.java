package net.lecousin.framework.concurrent.synch;

import java.util.Collection;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.ThreadingDebugHelper;
import net.lecousin.framework.exception.NoException;

/**
 * A JoinPoint allows to wait for several synchronization points or events, instead of a single one.
 * The methods addToJoin can be first called to add events to wait for.
 * Then, the method <b>start must be called</b>. At the time it is called, if all waited events are already done,
 * or the number of waited events is 0, this JoinPoint is immediately unblocked, else, each time an event is done, 
 * we check if this is the last one.<br/>
 * A JoinPoint can be used a single time, as a SynchronizationPoint. In other words, once it is unblocked, it remains unblocked.<br/>
 * Note that, like a SynchronizationPoint, if a JoinPoint is cancelled, or an error is given, the JoinPoint becomes
 * immediately unblocked, whatever there are remaining waited events or not, and whatever it has been started already or not.
 * @param <TError> type of exception it may raise
 */
public class JoinPoint<TError extends Exception> extends SynchronizationPoint<TError> {

	/** Constructor. */
	public JoinPoint() {
		if (Threading.debugSynchronization) ThreadingDebugHelper.register(this);
	}
	
	private int nbToJoin = 0;
	private boolean started = false;
	
	/** Return the number of <b>remaining</b> events this JoinPoint is waiting for. */
	public int getToJoin() {
		return nbToJoin;
	}

	/**
	 * Specify that we are waiting for the given number of additional events. The method joined will need to be called
	 * for every of those events, in order to unblock this JoinPoint.
	 */
	public synchronized void addToJoin(int nb) {
		nbToJoin += nb;
	}
	
	/**
	 * Register the given synchronization point as a waited event for this JoinPoint.<br/>
	 * The number of waited events is incremented, and a listener is added to the synchronization point and
	 * do the following when the synchronization point is unblocked:<ul>
	 * <li>Cancel this JoinPoint if the synchronization point is cancelled</li>
	 * <li>Unblock this JoinPoint with an error if the synchronization point has been unblocked with an error</li>
	 * <li>Call the joined method if the synchronization point has been unblocked with success</li>
	 * </ul>
	 */
	public synchronized void addToJoin(ISynchronizationPoint<? extends TError> sp) {
		nbToJoin++;
		sp.listenInline(new Runnable() {
			@Override
			public void run() {
				if (sp.isCancelled())
					cancel(sp.getCancelEvent());
				else if (sp.hasError())
					error(sp.getError());
				else
					joined();
			}
		});
		if (Threading.debugSynchronization) ThreadingDebugHelper.registerJoin(this, sp);
	}
	
	/**
	 * Register the given task as a waited event for this JoinPoint.<br/>
	 * Equivalent to addToJoin(task.getSynch())
	 */
	public synchronized void addToJoin(Task<?,? extends TError> task) {
		addToJoin(task.getSynch());
	}

	/**
	 * Register the given synchronization point as a waited event for this JoinPoint.<br/>
	 * The number of waited events is incremented, and a listener is added to the synchronization point.
	 * Once the synchronization point is unblock (whatever it succeed, it has an error or it has been cancelled)
	 * the joined method is called.
	 */
	public synchronized void addToJoinNoException(ISynchronizationPoint<?> sp) {
		nbToJoin++;
		sp.listenInline(new Runnable() {
			@Override
			public void run() {
				joined();
			}
		});
		if (Threading.debugSynchronization) ThreadingDebugHelper.registerJoin(this, sp);
	}
	
	/**
	 * Start this JoinPoint, so as soon as the number of waited events becomes zero, this JoinPoint becomes unblocked.
	 */
	public synchronized void start() {
		if (Threading.debugSynchronization) ThreadingDebugHelper.started(this);
		started = true;
		if (nbToJoin == 0) unblock();
	}
	
	/**
	 * Method to be called to signal that an event is done.<br/>
	 * The number of waited events is decremented, if it becomes 0 this JoinPoint is unblocked with success.
	 */
	public synchronized void joined() {
		if (nbToJoin == 0) {
			LCCore.getApplication().getDefaultLogger().error("JoinPoint: nbToJoin already 0", new Exception());
			return;
		}
		if (isUnblocked()) {
			nbToJoin--;
			if (!hasError() && !isCancelled())
				LCCore.getApplication().getDefaultLogger().error("JoinPoint: joined after timeout", new Exception());
			return;
		}
		if (--nbToJoin <= 0 && started) unblock();
	}
	
	/**
	 * Should be used only for debugging purpose, as a JoinPoint is supposed to always become unblocked.<br/>
	 * This method will wait for the given milliseconds, if at this time the JoinPoint is already unblocked,
	 * nothing is done, else we force it to become unblocked, and the given callback is called if any.
	 */
	public synchronized void timeout(long millis, Runnable callback) {
		if (isUnblocked()) return;
		Task<Void,NoException> task = new Task.Cpu<Void,NoException>("JoinPoint timeout", Task.PRIORITY_RATHER_LOW) {
			@Override
			public Void run() {
				synchronized (JoinPoint.this) {
					if (isUnblocked()) return null;
					if (callback != null)
						try { callback.run(); }
						catch (Throwable t) {
							LCCore.getApplication().getDefaultLogger().error("Error in callback of JoinPoint timeout", t);
						}
					unblock();
					return null;
				}
			}
		};
		task.executeIn(millis);
		if (isUnblocked()) return;
		task.start();
	}
	
	/**
	 * Call the given callback if this JoinPoint is still not yet unblocked after the given number of milliseconds.
	 * This method is similar to the method timeout, except that this JoinPoint is not unblocked when the timeout is reached.
	 */
	public synchronized void listenTime(long timeout, Runnable callback) {
		if (isUnblocked()) return;
		Task<Void,NoException> task = new Task.Cpu<Void,NoException>("JoinPoint timeout", Task.PRIORITY_RATHER_LOW) {
			@Override
			public Void run() {
				synchronized (JoinPoint.this) {
					if (isUnblocked()) return null;
					if (callback != null)
						try { callback.run(); }
						catch (Throwable t) {
							LCCore.getApplication().getDefaultLogger()
								.error("Error in callback of JoinPoint time listener", t);
						}
					return null;
				}
			}
		};
		task.executeIn(timeout);
		if (isUnblocked()) return;
		task.start();
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given synchronization points, <b>the JoinPoint is started by this method.</b>
	 */
	public static JoinPoint<Exception> fromSynchronizationPoints(ISynchronizationPoint<?>... synchPoints) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (int i = 0; i < synchPoints.length; ++i)
			jp.addToJoin(synchPoints[i]);
		jp.start();
		return jp;
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given synchronization points, <b>the JoinPoint is started by this method.</b>
	 */
	public static JoinPoint<Exception> fromSynchronizationPoints(Collection<? extends ISynchronizationPoint<?>> synchPoints) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (ISynchronizationPoint<?> sp : synchPoints)
			jp.addToJoin(sp);
		jp.start();
		return jp;
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given synchronization points, <b>the JoinPoint is started by this method.</b>
	 */
	@SafeVarargs
	public static <T extends Exception> JoinPoint<T> fromSynchronizationPointsSimilarError(ISynchronizationPoint<T>... synchPoints) {
		JoinPoint<T> jp = new JoinPoint<>();
		for (int i = 0; i < synchPoints.length; ++i)
			jp.addToJoin(synchPoints[i]);
		jp.start();
		return jp;
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given tasks, <b>the JoinPoint is started by this method.</b>
	 * If any task has error or is cancelled, the join point is immediately unblocked, even other tasks are still pending.
	 */
	public static JoinPoint<Exception> fromTasks(Task<?,?>... tasks) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (Task<?,?> task : tasks) jp.addToJoin(task.getSynch());
		jp.start();
		return jp;
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given tasks, <b>the JoinPoint is started by this method.</b>
	 * If any task has error or is cancelled, the join point is immediately unblocked, even other tasks are still pending.
	 */
	public static JoinPoint<Exception> fromTasks(Collection<? extends Task<?,?>> tasks) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (Task<?,?> task : tasks) jp.addToJoin(task.getSynch());
		jp.start();
		return jp;
	}

	/**
	 * Shortcut method to create a JoinPoint waiting for the given tasks, <b>the JoinPoint is started by this method.</b>
	 * The JoinPoint is not unblocked until all tasks are done. If any has error or is cancel, the error or cancellation
	 * reason is not given to the JoinPoint, in contrary of the method fromTasks.
	 */
	public static JoinPoint<NoException> onAllDone(Collection<? extends Task<?,?>> tasks) {
		JoinPoint<NoException> jp = new JoinPoint<>();
		jp.addToJoin(tasks.size());
		Runnable jpr = new Runnable() {
			@Override
			public void run() {
				jp.joined();
			}
		};
		jp.start();
		for (Task<?,?> t : tasks)
			t.getSynch().listenInline(jpr);
		return jp;
	}
	
	/**
	 * Shortcut method to create a JoinPoint waiting for the given synchronization points, start the JoinPoint,
	 * and add the given listener to be called when the JoinPoint is unblocked.
	 * If any synchronization point has an error or is cancelled, the JoinPoint is immediately unblocked.
	 */
	public static void listenInline(Runnable listener, ISynchronizationPoint<?>... synchPoints) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (int i = 0; i < synchPoints.length; ++i)
			jp.addToJoin(synchPoints[i]);
		jp.start();
		jp.listenInline(listener);
	}

	/**
	 * Shortcut method to create a JoinPoint waiting for the given synchronization points, start the JoinPoint,
	 * and add the given listener to be called when the JoinPoint is unblocked.
	 * The JoinPoint is not unblocked until all synchronization points are unblocked.
	 * If any has error or is cancel, the error or cancellation reason is not given to the JoinPoint,
	 * in contrary of the method listenInline.
	 */
	public static void listenInlineOnAllDone(Runnable listener, ISynchronizationPoint<?>... synchPoints) {
		JoinPoint<NoException> jp = new JoinPoint<>();
		jp.addToJoin(synchPoints.length);
		Runnable jpr = new Runnable() {
			@Override
			public void run() {
				jp.joined();
			}
		};
		for (int i = 0; i < synchPoints.length; ++i)
			synchPoints[i].listenInline(jpr);
		jp.start();
		jp.listenInline(listener);
	}
}
