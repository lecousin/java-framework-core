package net.lecousin.framework.progress;

import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.concurrent.synch.JoinPoint;

/** Implementation of WorkProgress, composed of sub-WorkProgress. */
public class MultiTaskProgress extends WorkProgressImpl implements WorkProgress.MultiTask {
	
	/** Constructor. */
	public MultiTaskProgress(String text) {
		super(0, text);
	}

	protected ArrayList<WorkProgress> tasks = new ArrayList<>();
	protected JoinPoint<Exception> jp = null;
	
	/** Create a sub-progress for the given amount of work (this amount is added to the total amount to be done). */
	public WorkProgress createTaskProgress(long amount, String text) {
		this.amount += amount;
		SubWorkProgress task = new SubWorkProgress(this, amount, amount, text);
		synchronized (tasks) {
			tasks.add(task);
			if (jp != null) jp.addToJoin(task.getSynch());
		}
		return task;
	}
	
	/** Add the given sub-progress as a sub-task for the given amount of work (this amount is added to the total amount to be done). */
	public void addTask(WorkProgress task, long amount) {
		this.amount += amount;
		synchronized (tasks) {
			tasks.add(task);
			if (jp != null) jp.addToJoin(task.getSynch());
		}
		WorkProgressUtil.propagateToParent(task, this, amount);
	}
	
	@Override
	public List<? extends WorkProgress> getTasks() {
		synchronized (tasks) {
			return new ArrayList<>(tasks);
		}
	}
	
	/** Automatically call the done or error method of this WorkProgress once all current sub-tasks are done. */
	public void doneOnSubTasksDone() {
		if (jp != null) return;
		synchronized (tasks) {
			jp = new JoinPoint<>();
			for (WorkProgress task : tasks) jp.addToJoin(task.getSynch());
		}
		jp.listenInline(new Runnable() {
			@Override
			public void run() {
				if (jp.hasError()) error(jp.getError());
				else done();
			}
		});
		jp.start();
	}
	
}
