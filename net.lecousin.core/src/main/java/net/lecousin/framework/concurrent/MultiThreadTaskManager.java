package net.lecousin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/** Implementation of TaskManager using several threads. */
public class MultiThreadTaskManager extends TaskManager {

	/** Constructor. */
	public MultiThreadTaskManager(
		String name, Object resource, int nbThreads, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		super(name, resource, nbThreads, threadFactory, taskPriorityManager);
		workers = new TaskWorker[nbThreads];
		for (int i = 0; i < nbThreads; ++i)
			workers[i] = new TaskWorker(name + " - Worker " + workerCount++, this);
	}
	
	private TaskWorker[] workers;
	private int workerCount = 1;
	
	@Override
	public void start() {
		for (int i = 0; i < workers.length; ++i)
			workers[i].thread.start();
	}
	
	@Override
	protected TaskWorker[] getWorkers() {
		return workers;
	}
	
	@Override
	protected void stopNow() {
		for (int i = getNbThreads() - 1; i >= 0; --i)
			workers[i].forceStop();
	}
	
	@Override
	protected void finishAndStop() {
		for (int i = getNbThreads() - 1; i >= 0; --i)
			workers[i].finishAndStop();
	}
	
	@Override
	boolean isStopped() {
		for (int i = getNbThreads() - 1; i >= 0; --i)
			if (workers[i].thread.isAlive()) return false;
		return true;
	}
	
	@Override
	protected TaskWorker createWorker() {
		return new TaskWorker(getName() + " - Worker " + workerCount++, this);
	}
	
	@Override
	protected void replaceWorkerBySpare(TaskWorker currentWorker, TaskWorker spareWorker) {
		synchronized (workers) {
			for (int i = getNbThreads() - 1; i >= 0; --i)
				if (workers[i] == currentWorker) {
					workers[i] = spareWorker;
					break;
				}
		}
	}
	
}
