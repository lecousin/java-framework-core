package net.lecousin.framework.progress;

import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.mutable.MutableLong;

/**
 * A WorkProgress allows to follow the progression of an asynchronous or background work.
 */
public interface WorkProgress {

	/** Set the total amount of work to be done. */
	public void setAmount(long work);
	
	/** Return the total amount of work to be done. */
	public long getAmount();
	
	/** Set the position (amount of work already done). */
	public void setPosition(long position);
	
	/** Return the current position, which is the current amount of work already done. */
	public long getPosition();
	
	/** Return the amount of remaining work. */
	public long getRemainingWork();
	
	/** Add an amount of work already done. */
	public void progress(long amountDone);
	
	/** Signal all the work has been done. */
	public void done();
	
	/** Signal an error. */
	public void error(Exception error);
	
	/** Cancel the underlying work. */
	public void cancel(CancelException reason);
	
	/** Return a synchronization point that will be unblocked by one of the method done, error or cancel. */
	public ISynchronizationPoint<Exception> getSynch();
	
	/** Add a listener to call on every change in the progress. */
	public void listen(Runnable onchange);

	/** Remove a listener to call on every change in the progress. */
	public void unlisten(Runnable onchange);
	
	/** Return the text describing the work being done. */
	public String getText();

	/** Set the text describing the work being done. */
	public void setText(String text);
	
	/** Return a sub-text describing the work being done. */
	public String getSubText();
	
	/** Set the sub-text describing the work being done. */
	public void setSubText(String text);
	
	/** Interface for a multi-task progress, which is composed of sub-WorkProgress. */
	public interface MultiTask {
		/** Return the sub-WorkProgress. */
		public List<? extends WorkProgress> getTasks();
	}
	
	/** Link this WorkProgress with the given synchronization point: once the synchronization point is unblocked,
	 * one of the done, error or cancel method is called.
	 */
	public static void linkTo(WorkProgress progress, ISynchronizationPoint<?> sp) {
		sp.listenInline(new Runnable() {
			@Override
			public void run() {
				if (sp.hasError()) progress.error(sp.getError());
				else if (sp.isCancelled()) progress.cancel(sp.getCancelEvent());
				else progress.done();
			}
		});
	}
	
	/** Link this WorkProgress with the given task: once the task is done,
	 * one of the done, error or cancel method is called.
	 */
	public static void linkTo(WorkProgress progress, Task<?,?> task) {
		linkTo(progress, task.getOutput());
	}
	
	/** Once the given sub-task is done, the given amount of work is added to the progress. */
	public static void link(WorkProgress subTask, WorkProgress progress, long work) {
		MutableLong sent = new MutableLong(0);
		subTask.getSynch().listenInline(new Runnable() {
			@Override
			public void run() {
				progress.progress(work - sent.get());
			}
		});
		subTask.listen(new Runnable() {
			@Override
			public void run() {
				long done = subTask.getPosition() * work / subTask.getAmount();
				if (sent.get() < done) {
					progress.progress(done - sent.get());
					sent.set(done);
				}
			}
		});
	}
	
}
