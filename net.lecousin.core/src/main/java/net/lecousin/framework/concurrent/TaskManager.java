package net.lecousin.framework.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.DebugUtil;

/**
 * Base class to implement a TaskManager, which is responsible to execute tasks in threads.
 */
public abstract class TaskManager {
	
	/** Constructor. */
	public TaskManager(
		String name, Object resource, int nbThreads, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		this.name = name;
		this.resource = resource;
		this.nbThreads = nbThreads;
		this.threadFactory = threadFactory;
		try { this.taskPriorityManager = taskPriorityManager.newInstance(); }
		catch (Throwable t) {
			throw new RuntimeException("Unable to instantiate TaskPriorityManager", t);
		}
		this.taskPriorityManager.setTaskManager(this);
		spare = new TurnArray<TaskWorker>(nbThreads * 2);
		blocked = new TurnArray<TaskWorker>(nbThreads);
	}
	
	private String name;
	private Object resource;
	private int nbThreads;
	private ThreadFactory threadFactory;
	private TaskPriorityManager taskPriorityManager;
	
	private TurnArray<TaskWorker> spare;
	private TurnArray<TaskWorker> blocked;
	private LinkedList<TaskWorker> aside = new LinkedList<>();
	private Object stopping = null;
	private TaskManager transferredTo = null;
	private boolean stopped = false;
	private LinkedList<AsyncWork<TaskWorker,NoException>> pausesToDo = new LinkedList<>();
	
	public String getName() { return name; }
	
	public void setName(String name) { this.name = name; }
	
	public int getNbThreads() { return nbThreads; }
	
	public Object getResource() { return resource; }
	
	/** Start the threads of this task manager. */
	public abstract void start();
	
	Thread newThread(TaskWorker worker) {
		return threadFactory.newThread(worker);
	}
	
	public TaskManager getTransferTarget() {
		return transferredTo;
	}
	
	final void autoCloseSpares() {
		new CloseOldSpare().start();
	}

	final void shutdownWhenNoMoreTasks() {
		stopping = new Object();
		Thread t = new Thread("Stopping Task Manager: " + name) {
			@Override
			public void run() {
				Threading.logger.info("   * Stopping Task Manager: " + name);
				do {
					synchronized (taskPriorityManager) {
						if (!taskPriorityManager.hasRemainingTasks(false)) {
							// no more task => shutdown
							Threading.logger.info("   * Task Manager has no more task to do: " + name);
							TaskManager.this.finishAndStop();
							synchronized (spare) {
								while (!spare.isEmpty())
									spare.removeFirst().forceStop();
							}
							taskPriorityManager.notifyAll();
							break;
						}
					}
					Threading.logger.info("   * Waiting for task manager " + name + " to finish its tasks");
					synchronized (stopping) {
						try { stopping.wait(1000); }
						catch (InterruptedException e) { break; }
					}
				} while (true);
			}
		};
		t.start();
	}
	
	final void forceStop() {
		synchronized (taskPriorityManager) {
			TaskManager.this.stopNow();
			synchronized (spare) {
				while (!spare.isEmpty())
					spare.removeFirst().forceStop();
			}
			taskPriorityManager.forceStop();
		}
	}
	
	final void transferAndClose(TaskManager transferTo) {
		Threading.logger.info("Transferring TaskManager " + this.name + " to " + transferTo.name);
		// new tasks will go to the new one
		transferredTo = transferTo;
		// transfer what is ready
		List<Task<?,?>> list;
		synchronized (taskPriorityManager) {
			list = taskPriorityManager.removeAllPendingTasks();
		}
		for (Task<?,?> t : list) {
			Threading.logger.debug("  - Task ready " + t.description + " transferred to " + transferTo.name);
			transferTo.addReady(t);	
		}
		// finish pending work
		synchronized (taskPriorityManager) {
			finishAndStop();
			taskPriorityManager.notifyAll();
		}
		do {
			AsyncWork<TaskWorker,NoException> waitPause = getPauseToDo();
			if (waitPause == null)
				break;
			Threading.logger.debug("  - Finish blocked task ");
			waitPause.unblockSuccess(null);
		} while (true);
		// stop the workers and spares
		synchronized (taskPriorityManager) {
			finishAndStop();
			synchronized (spare) {
				while (!spare.isEmpty())
					spare.removeFirst().finishAndStop();
			}
			taskPriorityManager.notifyAll();
		}
		synchronized (blocked) {
			for (TaskWorker w : blocked)
				Threading.logger.error("  - Remaining blocked thread: " + w.thread.getName());
		}
		Threading.logger.info("End of transfer for TaskManager " + this.name + " to " + transferTo.name);
		stopped = true;
	}
	
	final void cancelAndStop() {
		stopped = true;
		// stop the workers and spares
		synchronized (taskPriorityManager) {
			stopNow();
			synchronized (spare) {
				while (!spare.isEmpty())
					spare.removeFirst().forceStop();
			}
			taskPriorityManager.notifyAll();
		}
		// cancel what is ready
		List<Task<?,?>> list;
		synchronized (taskPriorityManager) {
			list = taskPriorityManager.removeAllPendingTasks();
		}
		for (Task<?,?> t : list)
			t.cancel(new CancelException("Stop Task Manager", null));
	}
	
	final int getRemainingTasks(boolean includingBackground) {
		return taskPriorityManager.getRemainingTasks(includingBackground);
	}
	
	protected abstract void finishAndStop();
	
	protected abstract void stopNow();
	
	abstract boolean isStopped();
	
	final boolean isStopping() {
		return stopping != null;
	}
	
	final void addReady(Task<?,?> t) {
		if (stopped)
			t.cancel(new CancelException("Task Manager already stopped", null));
		taskPriorityManager.add(t);
	}
	
	final Task<?,?> peekNextOrWait(TaskWorker w) {
		try {
			synchronized (taskPriorityManager) {
				if (!pausesToDo.isEmpty()) return null;
				return taskPriorityManager.peekNextOrWait(w);
			}
		} finally {
			if (stopping != null) synchronized (stopping) { stopping.notify(); }
		}
	}
	
	protected abstract TaskWorker createWorker();
	
	protected abstract void replaceWorkerBySpare(TaskWorker currentWorker, TaskWorker spareWorker);
	
	protected abstract TaskWorker[] getWorkers();
	
	protected void addSpare(TaskWorker worker) {
		synchronized (spare) {
			spare.addLast(worker);
		}
	}
	
	AsyncWork<TaskWorker,NoException> getPauseToDo() {
		if (pausesToDo.isEmpty()) return null;
		synchronized (pausesToDo) {
			if (pausesToDo.isEmpty()) return null;
			return pausesToDo.removeFirst();
		}
	}
	
	@SuppressFBWarnings("NN_NAKED_NOTIFY")
	void imBlocked(TaskWorker worker) {
		if (Threading.traceBlockingTasks) {
			Threading.logger.error("Task " + worker.currentTask.description + " blocked", new Exception());
		}
		if (TaskMonitoring.checkLocksOfBlockingTasks)
			TaskMonitoring.checkNoLockForWorker();
		if (transferredTo != null) {
			// we are in the process of being transferred, we cannot launch a spare
			Threading.logger.info("Task blocked while transferring to a new TaskManager: " + worker.currentTask.description);
			synchronized (blocked) {
				blocked.addLast(worker);
			}
			return;
		}
		// a thread is blocked, we need to launch a spare
		TaskWorker w;
		synchronized (spare) {
			w = spare.pollFirst();
		}
		if (w == null) {
			// no more spare !
			w = createWorker();
			// replace the worker
			replaceWorkerBySpare(worker, w);
			// start the new one
			w.thread.start();
		} else {
			// replace the worker
			replaceWorkerBySpare(worker, w);
			// wake up this spare
			synchronized (w) { w.notify(); }
		}
		synchronized (blocked) {
			blocked.addLast(worker);
		}
	}
	
	void imUnblocked(TaskWorker worker, long since) {
		if (Threading.traceBlockingTasks) {
			Threading.logger.error("Task " + worker.currentTask.description + " unblocked after "
				+ ((System.nanoTime() - since) / 1000000) + "ms.");
		}
		// pause the next worker as soon as it is done with its work
		AsyncWork<TaskWorker,NoException> pause = new AsyncWork<>();
		synchronized (taskPriorityManager) {
			synchronized (pausesToDo) {
				pausesToDo.add(pause);
			}
			taskPriorityManager.notify();
		}
		if (!stopped)
			pause.blockPause(30000);
		// replace paused worker
		if (pause.getResult() != null) { // can be null if we are stopping and just want to unblock this thread
			replaceWorkerBySpare(pause.getResult(), worker);
			synchronized (spare) {
				spare.addLast(pause.getResult());
			}
		}
		synchronized (blocked) {
			blocked.removeInstance(worker);
		}
	}
	
	boolean remove(Task<?,?> task) {
		synchronized (taskPriorityManager) {
			if (taskPriorityManager.remove(task))
				return true;
		}
		if (transferredTo != null) {
			do {
				task.manager = task.manager.transferredTo;
			} while (task.manager.transferredTo != null);
			synchronized (task.manager.taskPriorityManager) {
				return task.manager.taskPriorityManager.remove(task);
			}
		}
		return false;
	}
	
	List<TaskWorker> getAllActiveWorkers() {
		TaskWorker[] workers = getWorkers();
		ArrayList<TaskWorker> list = new ArrayList<>(workers.length + blocked.size() + aside.size());
		for (TaskWorker w : workers)
			list.add(w);
		synchronized (blocked) {
			list.addAll(blocked);
		}
		synchronized (aside) {
			list.addAll(aside);
		}
		return list;
	}
	
	void putWorkerAside(TaskWorker worker) {
		TaskWorker newWorker = createWorker();
		worker.aside = true;
		synchronized (aside) {
			aside.add(worker);
			replaceWorkerBySpare(worker, newWorker);
		}
		newWorker.thread.start();
	}
	
	@SuppressWarnings("deprecation")
	void killWorker(TaskWorker worker) {
		synchronized (aside) {
			if (!aside.remove(worker)) return;
		}
		StackTraceElement[] stack = worker.thread.getStackTrace();
		StringBuilder s = new StringBuilder(1024);
		s.append("Task stopped at \r\n");
		DebugUtil.createStackTrace(s, stack);
		Threading.logger.error(s.toString());
		worker.thread.stop();
		if (worker.currentTask != null)
			worker.currentTask.getSynch().cancel(new CancelException("Task was running since a too long time"));
	}
	
	void asideWorkerDone(TaskWorker worker) {
		synchronized (aside) {
			aside.remove(worker);
		}
	}

	/** Describe what threads are doing for debugging purpose. */
	public void debug(StringBuilder s) {
		s.append("Task Manager: ").append(name).append(" (").append(nbThreads).append(" threads):\r\n");
		for (TaskWorker w : getWorkers())
			w.debug(s, "Worker");
		for (TaskWorker w : spare)
			try { w.debug(s, "Spare"); }
			catch (Throwable t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
		for (TaskWorker w : blocked)
			try { w.debug(s, "Blocked"); }
			catch (Throwable t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
	}
	
	/** Print statistics to the given StringBuilder. */
	public void printStats(StringBuilder s) {
		try {
			s.append("Task Manager: ").append(name).append(" (").append(nbThreads).append(" threads):\r\n");
			for (TaskWorker w : getWorkers()) {
				s.append(" - Worker ");
				w.printStats(s);
			}
			for (TaskWorker w : spare) {
				s.append(" - Spare ");
				w.printStats(s);
			}
			for (TaskWorker w : blocked) {
				s.append(" - Blocked ");
				w.printStats(s);
			}
		} catch (Throwable t) {
			/* ignore */
		}
	}
	
	private class CloseOldSpare extends Task.Cpu<Void,NoException> {
		private CloseOldSpare() {
			super("Close old spare threads for " + getName(), Task.PRIORITY_BACKGROUND);
			executeEvery(60000, 6 * 60000);
		}
		
		@Override
		public Void run() {
			synchronized (spare) {
				if (spare.size() <= nbThreads) return null;
				for (Iterator<TaskWorker> it = spare.iterator(); it.hasNext(); ) {
					TaskWorker t = it.next();
					if (t.lastUsed > 5 * 60000) {
						Threading.logger.info("Spare thread not used since more than 5 minutes => stop it");
						t.forceStop();
						spare.removeInstance(t);
						return null;
					}
				}
			}
			return null;
		}
	}
}
