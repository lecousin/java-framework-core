package net.lecousin.framework.progress;

import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.JoinPoint;

/** Implementation of WorkProgress, composed of sub-WorkProgress. */
public class MultiTaskProgress extends WorkProgressImpl implements WorkProgress.MultiTask {
	
	/** Constructor. */
	public MultiTaskProgress(String text) {
		super(0, text);
	}

	protected ArrayList<SubTask> tasks = new ArrayList<>();
	protected JoinPoint<Exception> jp = null;
	
	/** Create a sub-progress for the given amount of work (this amount is added to the total amount to be done). */
	public WorkProgress createTaskProgress(long amount, String text) {
		this.amount += amount;
		SubWorkProgress task = new SubWorkProgress(this, amount, amount, text);
		synchronized (tasks) {
			tasks.add(task);
			if (jp != null) jp.addToJoinDoNotCancel(task.getSynch());
		}
		return task;
	}
	
	@Override
	public SubTask addTask(WorkProgress task, long amount) {
		SubTask.Wrapper wrapper = new SubTask.Wrapper(task, amount);
		this.amount += amount;
		synchronized (tasks) {
			tasks.add(wrapper);
			if (jp != null) jp.addToJoinDoNotCancel(task.getSynch());
		}
		WorkProgressUtil.propagateToParent(task, this, amount);
		return wrapper;
	}

	@Override
	public void removeTask(SubTask subTask) {
		synchronized (tasks) {
			tasks.remove(subTask);
			if (jp != null && !subTask.getProgress().getSynch().isDone())
				subTask.getProgress().getSynch().cancel(new CancelException("Sub-task removed"));
		}
	}
	
	@Override
	public List<? extends SubTask> getTasks() {
		synchronized (tasks) {
			return new ArrayList<>(tasks);
		}
	}
	
	/** Automatically call the done or error method of this WorkProgress once all current sub-tasks are done. */
	public void doneOnSubTasksDone() {
		if (jp != null) return;
		synchronized (tasks) {
			jp = new JoinPoint<>();
			for (SubTask task : tasks) jp.addToJoinDoNotCancel(task.getProgress().getSynch());
		}
		jp.onDone(() -> {
			if (jp.hasError()) error(jp.getError());
			else done();
		});
		jp.start();
	}
	
}
