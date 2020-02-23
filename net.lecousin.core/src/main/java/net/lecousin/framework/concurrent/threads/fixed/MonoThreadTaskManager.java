package net.lecousin.framework.concurrent.threads.fixed;

import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.concurrent.threads.TaskManagerMonitor;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;

/** Implementation of a TaskManager using a single thread. */
public class MonoThreadTaskManager extends FixedThreadTaskManager {

	/** Constructor. */
	public MonoThreadTaskManager(
		String name, Object resource, ThreadFactory threadFactory, TaskPriorityManager taskPriorityManager,
		TaskManagerMonitor.Configuration monitorConfig
	) {
		super(name, resource, 1, threadFactory, taskPriorityManager, monitorConfig);
		
		worker = new TaskWorker(name + " - Worker 1", this);
	}
	
	private TaskWorker worker;
	private int workerCount = 2;
	
	@Override
	protected void startThreads() {
		worker.getThread().start();
	}
	
	@Override
	protected TaskWorker[] getWorkers() {
		return new TaskWorker[] { worker };
	}
	
	@Override
	protected void forceStopWorkers() {
		worker.forceStop(false);
	}
	
	@Override
	protected void finishAndStopWorkers() {
		worker.finishAndStop();
	}
	
	@Override
	public boolean allActiveExecutorsStopped() {
		return !worker.getThread().isAlive();
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
