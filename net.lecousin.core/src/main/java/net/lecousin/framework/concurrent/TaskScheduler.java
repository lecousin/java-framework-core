package net.lecousin.framework.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.RedBlackTreeLongByRange;

class TaskScheduler extends Thread {
	
	private TaskScheduler() {
		super("Task Scheduler");
	}
	
	static TaskScheduler instance;
	
	static void init() {
		instance = new TaskScheduler();
		instance.start();
	}
	
	private boolean stop = false;
	static boolean stopping = false;
	private RedBlackTreeLongByRange<Task<?,?>> waitingTime = new RedBlackTreeLongByRange<>(2 * 60 * 1000);
	long waitingNano = 0;
	long busyNano = 0;
	long nbRounds = 0;
	
	static void schedule(Task<?,?> task) {
		synchronized (instance) {
			boolean first = instance.waitingTime.isEmpty() || task.nextExecution < instance.waitingTime.getMin().getValue();
			instance.waitingTime.add(task.nextExecution, task);
			task.status = Task.STATUS_STARTED_WAITING;
			if (first) instance.notify();
		}
	}
	
	static boolean cancel(Task<?,?> task) {
		synchronized (instance) {
			if (instance.waitingTime.containsInstance(task.nextExecution, task)) {
				instance.waitingTime.removeInstance(task.nextExecution, task);
				return true;
			}
		}
		return false;
	}
	
	static void changeNextExecutionTime(Task<?,?> t, long time) {
		// we are called in a synchronized method of the task
		synchronized (instance) {
			if (t.status == Task.STATUS_STARTED_WAITING) {
				// still waiting
				boolean needWakeUp = instance.waitingTime.getMin().getElement() == t;
				instance.waitingTime.removeInstance(t.nextExecution, t);
				t.nextExecution = time;
				if (!needWakeUp && (instance.waitingTime.isEmpty() || instance.waitingTime.getMin().getValue() > time))
					needWakeUp = true;
				instance.waitingTime.add(time, t);
				if (needWakeUp)
					instance.notify();
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
						instance.notify();
				}
			}
		}
	}
	
	@SuppressFBWarnings("NN_NAKED_NOTIFY")
	static void end() {
		instance.stop = true;
		synchronized (instance) { instance.notify(); }
	}
	
	@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
	@Override
	public void run() {
		long start = System.nanoTime();
		while (!stop) {
			nbRounds++;
			try {
				synchronized (this) {
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
						this.wait(timeout);
						start = System.nanoTime();
						waitingNano += start - now;
						continue;
					} catch (InterruptedException e) { break; }
				}
			} catch (Throwable t) {
				Threading.logger.error("Error in Task Scheduler", t);
			}
		}
		System.out.println("Task Scheduler stopped: was busy " + ((double)busyNano) / 1000000000
			+ ", was waiting " + ((double)waitingNano) / 1000000000 + ", did " + nbRounds + " rounds");
	}
	
}
