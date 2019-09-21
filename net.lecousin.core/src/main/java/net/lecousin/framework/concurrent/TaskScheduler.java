package net.lecousin.framework.concurrent;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.RedBlackTreeLongByRange;

class TaskScheduler extends Thread {
	
	private TaskScheduler() {
		super("Task Scheduler");
	}
	
	private static TaskScheduler instance;
	private static Object lock;
	
	static void init() {
		lock = new Object();
		instance = new TaskScheduler();
		instance.start();
	}
	
	private boolean stop = false;
	static boolean stopping = false;
	private RedBlackTreeLongByRange<Task<?,?>> waitingTime = new RedBlackTreeLongByRange<>(2L * 60 * 1000);
	long waitingNano = 0;
	long busyNano = 0;
	long nbRounds = 0;
	
	static void schedule(Task<?,?> task) {
		synchronized (lock) {
			boolean first = instance.waitingTime.isEmpty() || task.nextExecution < instance.waitingTime.getMin().getValue();
			instance.waitingTime.add(task.nextExecution, task);
			task.status = Task.STATUS_STARTED_WAITING;
			if (first) lock.notify();
		}
	}
	
	static boolean cancel(Task<?,?> task) {
		synchronized (lock) {
			if (instance.waitingTime.containsInstance(task.nextExecution, task)) {
				instance.waitingTime.removeInstance(task.nextExecution, task);
				return true;
			}
		}
		return false;
	}
	
	static void changeNextExecutionTime(Task<?,?> t, long time) {
		// we are called in a synchronized method of the task
		synchronized (lock) {
			if (t.status == Task.STATUS_STARTED_WAITING) {
				// still waiting
				boolean needWakeUp = instance.waitingTime.getMin().getElement() == t;
				instance.waitingTime.removeInstance(t.nextExecution, t);
				t.nextExecution = time;
				if (!needWakeUp && (instance.waitingTime.isEmpty() || instance.waitingTime.getMin().getValue() > time))
					needWakeUp = true;
				instance.waitingTime.add(time, t);
				if (needWakeUp)
					lock.notify();
				return;
			}
			if (t.status == Task.STATUS_STARTED_READY) {
				t.manager.remove(t);
				t.nextExecution = time;
				t.status = Task.STATUS_STARTED_WAITING;
				if (time <= System.currentTimeMillis())
					t.sendToTaskManager();
				else {
					boolean needWakeUp = instance.waitingTime.isEmpty() || instance.waitingTime.getMin().getValue() > time;
					instance.waitingTime.add(time, t);
					if (needWakeUp)
						lock.notify();
				}
			}
		}
	}
	
	static void end() {
		instance.stop = true;
		synchronized (lock) { lock.notify(); }
	}
	
	@Override
	@SuppressWarnings({
		"squid:S106", // print to console
		"squid:S1141", // nested try
		"squid:S2142" // InterruptedException
	})
	public void run() {
		long start = System.nanoTime();
		while (!stop) {
			nbRounds++;
			try {
				synchronized (lock) {
					long timeout = 0;
					if (!waitingTime.isEmpty()) {
						long now = System.currentTimeMillis();
						do {
							RedBlackTreeLong.Node<Task<?,?>> min = waitingTime.getMin();
							if (min.getValue() > now) {
								timeout = min.getValue() - now;
								break;
							}
							Task<?,?> task = min.getElement();
							waitingTime.removeMin();
							if (task.isCancelled()) continue;
							if (task.status != Task.STATUS_STARTED_WAITING) {
								Threading.logger.debug("Scheduled task is not in a waiting status (" + task.status
									+ "), so the scheduler does not start it");
								continue;
							}
							task.sendToTaskManager();
						} while (!waitingTime.isEmpty());
					}
					long now = System.nanoTime();
					busyNano += now - start;
					try {
						lock.wait(timeout);
						start = System.nanoTime();
						waitingNano += start - now;
					} catch (InterruptedException e) { break; }
				}
			} catch (Exception t) {
				Threading.logger.error("Error in Task Scheduler", t);
			}
		}
		System.out.println("Task Scheduler stopped: was busy " + ((double)busyNano) / 1000000000
			+ ", was waiting " + ((double)waitingNano) / 1000000000 + ", did " + nbRounds + " rounds");
	}
	
}
