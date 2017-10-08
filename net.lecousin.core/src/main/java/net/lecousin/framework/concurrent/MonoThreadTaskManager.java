package net.lecousin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/** Implementation of a TaskManager using a single thread. */
public class MonoThreadTaskManager extends TaskManager {

	/** Constructor. */
	public MonoThreadTaskManager(
		String name, Object resource, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		super(name, resource, 1, threadFactory, taskPriorityManager);
		
		worker = new TaskWorker(name + " - Worker 1", this);
	}
	
	private TaskWorker worker;
	private int workerCount = 2;
	
	@Override
	public void start() {
		worker.thread.start();
	}
	
	@Override
	protected TaskWorker[] getWorkers() {
		return new TaskWorker[] { worker };
	}
	
	@Override
	protected void stopNow() {
		worker.forceStop();
	}
	
	@Override
	protected void finishAndStop() {
		worker.finishAndStop();
	}
	
	@Override
	boolean isStopped() {
		return !worker.thread.isAlive();
	}
	
	@Override
	protected TaskWorker createWorker() {
		return new TaskWorker(getName() + " - Worker " + workerCount++, this);
	}
	
	@Override
	protected void replaceWorkerBySpare(TaskWorker currentWorker, TaskWorker spareWorker) {
		worker = spareWorker;
	}
	
}
