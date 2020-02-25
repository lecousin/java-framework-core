package net.lecousin.framework.concurrent.threads.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.TaskExecutor;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.TaskManagerMonitor;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;

/**
 * A Task Manager that use a pool of threads to execute tasks.
 * This is used for unmanaged tasks, that may use several physical resources and block at any time,
 * typically when using functionalities from a library that is not designed to be used with LCCore.
 * Each time a new task comes, if the number of active threads is less than the maximum number of threads,
 * a new thread is launched to execute the task, else it is queued.
 * When a thread finishes a task, it checkes if tasks are present in the queue. If yes, it peeks one and
 * execute it. Else the thread ends.
 */
public class ThreadPoolTaskManager extends TaskManager {

	/** Constructor. */
	public ThreadPoolTaskManager(
		String name, Object resource, int maxThreads, ThreadFactory threadFactory, TaskPriorityManager taskPriorityManager,
		TaskManagerMonitor.Configuration monitorConfig
	) {
		super(name, resource, threadFactory, taskPriorityManager, monitorConfig);
		if (maxThreads > 0)
			this.maxThreads = maxThreads;
		else
			this.maxThreads = 100;
	}
	
	private int maxThreads;
	LinkedList<TaskWorker> active = new LinkedList<>();
	long tasksDone = 0;
	long tasksTime = 0;
	private long threadCounter = 1;
	
	private String newThreadName() {
		return "ThreadPool " + getName() + " - " + (threadCounter++);
	}
	
	TaskPriorityManager getPriorityManager() {
		return taskPriorityManager;
	}
	
	@Override
	protected void startThreads() {
		// nothing to do
	}
	
	@Override
	protected void threadingStarted() {
		// nothing to do
	}
	
	@Override
	protected void finishAndStopActiveAndInactiveExecutors() {
		// nothing to do
	}
	
	@Override
	protected void forceStopActiveAndInactiveExecutors() {
		synchronized (taskPriorityManager) {
			for (TaskWorker w : active)
				w.forceStop = true;
			taskPriorityManager.notifyAll();
		}
	}
	
	@Override
	public boolean allActiveExecutorsStopped() {
		synchronized (taskPriorityManager) {
			return active.isEmpty();
		}
	}
	
	@Override
	protected void add(Task<?, ?> t) {
		synchronized (taskPriorityManager) {
			if (active.size() < maxThreads)
				active.add(new TaskWorker(t, this, newThreadName()));
			else
				taskPriorityManager.add(t);
		}
	}
	
	@Override
	protected void executorUncaughtException(TaskExecutor executor) {
		synchronized (taskPriorityManager) {
			active.remove(executor);
			Task<?,?> task = taskPriorityManager.peekNext();
			if (task != null)
				active.add(new TaskWorker(task, this, newThreadName()));
		}
	}
	
	@Override
	protected void replaceBlockedExecutor(TaskExecutor executor) {
		synchronized (taskPriorityManager) {
			active.remove(executor);
			Task<?,?> task = taskPriorityManager.peekNext();
			if (task != null)
				active.add(new TaskWorker(task, this, newThreadName()));
		}
	}
	
	@Override
	protected void unblockedExecutor(TaskExecutor executor) {
		synchronized (taskPriorityManager) {
			active.add((TaskWorker)executor);
		}
	}
	
	@Override
	protected void executorAside(TaskExecutor executor) {
		synchronized (taskPriorityManager) {
			active.remove(executor);
			Task<?,?> task = taskPriorityManager.peekNext();
			if (task != null)
				active.add(new TaskWorker(task, this, newThreadName()));
		}
	}
	
	@Override
	public List<TaskExecutor> getActiveExecutors() {
		synchronized (taskPriorityManager) {
			return new ArrayList<>(active);
		}
	}
	
	@Override
	public List<TaskExecutor> getInactiveExecutors() {
		return Collections.emptyList();
	}
	
	@Override
	protected void getDebugDescription(StringBuilder s) {
		s.append("Thread pool ").append(getName()).append(": ")
		 .append(active.size()).append('/').append(maxThreads).append(" active threads");
	}

}
