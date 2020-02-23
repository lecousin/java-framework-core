package net.lecousin.framework.progress;

import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.event.SimpleListenable;
import net.lecousin.framework.mutable.MutableLong;

/**
 * A WorkProgress allows to follow the progression of an asynchronous or background work.
 */
public interface WorkProgress extends SimpleListenable {

	/** Set the total amount of work to be done. */
	void setAmount(long work);
	
	/** Return the total amount of work to be done. */
	long getAmount();
	
	/** Set the position (amount of work already done). */
	void setPosition(long position);
	
	/** Return the current position, which is the current amount of work already done. */
	long getPosition();
	
	/** Return the amount of remaining work. */
	long getRemainingWork();
	
	/** Add an amount of work already done. */
	void progress(long amountDone);
	
	/** Signal all the work has been done. */
	void done();
	
	/** Signal an error. */
	void error(Exception error);
	
	/** Cancel the underlying work. */
	void cancel(CancelException reason);
	
	/** Return a synchronization point that will be unblocked by one of the method done, error or cancel. */
	IAsync<Exception> getSynch();
	
	/** Stop triggering events to listener, this may be useful before doing several modifications. */
	void interruptEvents();
	
	/** Resume triggering events after a call to interruptEvents(), and optionally trigger an event now. */
	void resumeEvents(boolean trigger);
	
	/** Return the text describing the work being done. */
	String getText();

	/** Set the text describing the work being done. */
	void setText(String text);
	
	/** Return a sub-text describing the work being done. */
	String getSubText();
	
	/** Set the sub-text describing the work being done. */
	void setSubText(String text);
	
	/** Interface for a multi-task progress, which is composed of sub-WorkProgress. */
	public interface MultiTask {
		
		/** Interface for a sub-task. */
		public interface SubTask {
			/** Amount of work on the parent done by this sub-task. */
			long getWorkOnParent();
			
			/** Progress of this sub-task. */
			WorkProgress getProgress();
			
			/** Simple implementation of SubTask. */
			public static class Wrapper implements SubTask {
				
				/** Constructor. */
				public Wrapper(WorkProgress progress, long amount) {
					this.progress = progress;
					this.amount = amount;
				}
				
				protected WorkProgress progress;
				protected long amount;

				@Override
				public long getWorkOnParent() {
					return amount;
				}

				@Override
				public WorkProgress getProgress() {
					return progress;
				}
				
			}
		}
		
		/** Return the sub-WorkProgress. */
		List<? extends SubTask> getTasks();
		
		/** Add the given sub-progress as a sub-task for the given amount of work (this amount is added to the total amount to be done). */
		SubTask addTask(WorkProgress task, long amount);
		
		/** Remove a sub-task, but the amount of the parent remains unchanged. */
		void removeTask(SubTask subTask);
	}
	
	/** Link this WorkProgress with the given synchronization point: once the synchronization point is unblocked,
	 * one of the done, error or cancel method is called.
	 */
	static void linkTo(WorkProgress progress, IAsync<?> sp) {
		sp.onDone(() -> {
			if (sp.hasError()) progress.error(sp.getError());
			else if (sp.isCancelled()) progress.cancel(sp.getCancelEvent());
			else progress.done();
		});
	}
	
	/** Link this WorkProgress with the given task: once the task is done,
	 * one of the done, error or cancel method is called.
	 */
	static void linkTo(WorkProgress progress, Task<?,?> task) {
		linkTo(progress, task.getOutput());
	}
	
	/** Once the given sub-task is done, the given amount of work is added to the progress. */
	static void link(WorkProgress subTask, WorkProgress progress, long work) {
		MutableLong sent = new MutableLong(0);
		subTask.getSynch().onDone(() -> {
			if (subTask.getSynch().hasError())
				progress.error(subTask.getSynch().getError());
			else if (subTask.getSynch().isCancelled())
				progress.cancel(subTask.getSynch().getCancelEvent());
			else {
				progress.progress(work - sent.get());
				sent.set(work);
			}
		});
		if (subTask.getSynch().isDone()) return;
		Runnable listener = () -> {
			long done = subTask.getPosition() * work / subTask.getAmount();
			if (sent.get() < done && !progress.getSynch().isDone()) {
				progress.progress(done - sent.get());
				sent.set(done);
			}
		};
		listener.run();
		subTask.listen(listener);
	}
	
}
