package net.lecousin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/** Implementation of TaskManager using several threads. */
public class MultiThreadTaskManager extends FixedThreadTaskManager {

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
	protected void forceStopWorkers() {
		for (int i = getNbThreads() - 1; i >= 0; --i)
			if (workers[i].thread.isAlive())
				workers[i].forceStop(false);
	}
	
	@Override
	protected void finishAndStopWorkers() {
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
		int nb;
		synchronized (this) { nb = workerCount++; }
		return new TaskWorker(getName() + " - Worker " + nb, this);
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
