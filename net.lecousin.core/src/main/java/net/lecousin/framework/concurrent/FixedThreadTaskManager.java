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
public abstract class FixedThreadTaskManager extends TaskManager {
	
	/** Constructor. */
	public FixedThreadTaskManager(
		String name, Object resource, int nbThreads, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		super(name, resource, threadFactory, taskPriorityManager);
		this.nbThreads = nbThreads;
		spare = new TurnArray<TaskWorker>(nbThreads * 2);
		blocked = new TurnArray<TaskWorker>(nbThreads);
	}
	
	private int nbThreads;
	private TurnArray<TaskWorker> spare;
	private TurnArray<TaskWorker> blocked;
	private LinkedList<TaskWorker> aside = new LinkedList<>();
	private LinkedList<AsyncWork<TaskWorker,NoException>> pausesToDo = new LinkedList<>();
	
	public int getNbThreads() { return nbThreads; }
	
	@Override
	void started() {
		new CloseOldSpare().start();
	}
	
	@Override
	protected final void finishAndStopThreads() {
		// stop workers
		finishAndStopWorkers();
		// stop spares
		synchronized (spare) {
			while (!spare.isEmpty())
				spare.removeFirst().forceStop();
		}
		// resume blocked threads
		do {
			AsyncWork<TaskWorker,NoException> waitPause = getPauseToDo();
			if (waitPause == null)
				break;
			waitPause.unblockSuccess(null);
		} while (true);
	}
	
	protected abstract void finishAndStopWorkers();
	
	@Override
	protected void forceStopThreads() {
		forceStopWorkers();
		synchronized (spare) {
			while (!spare.isEmpty())
				spare.removeFirst().forceStop();
		}
	}
	
	protected abstract void forceStopWorkers();
	
	@Override
	protected void finishTransfer() {
		// TODO transfer blocked
		synchronized (blocked) {
			for (TaskWorker w : blocked)
				Threading.logger.error("  - Remaining blocked thread: " + w.thread.getName());
		}
	}

	Thread newThread(TaskWorker worker) {
		return threadFactory.newThread(worker);
	}
	
	final Task<?,?> peekNextOrWait() {
		try {
			synchronized (taskPriorityManager) {
				if (!pausesToDo.isEmpty()) return null;
				return taskPriorityManager.peekNextOrWait();
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
	
	List<TaskWorker> getBlockedWorkers() {
		synchronized (blocked) {
			return new ArrayList<>(blocked);
		}
	}
	
	void putWorkerAside(TaskWorker worker) {
		if (worker.aside) return;
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
			worker.currentTask.cancel(new CancelException("Task was running since a too long time"));
	}
	
	void asideWorkerDone(TaskWorker worker) {
		synchronized (aside) {
			aside.remove(worker);
		}
	}

	@Override
	public void debug(StringBuilder s) {
		try {
			s.append("Task Manager: ").append(getName()).append(" (").append(nbThreads).append(" threads):\r\n");
			for (TaskWorker w : getWorkers())
				w.debug(s, "Worker");
			for (TaskWorker w : spare)
				try { w.debug(s, "Spare"); }
				catch (Throwable t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
			for (TaskWorker w : blocked)
				try { w.debug(s, "Blocked"); }
				catch (Throwable t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
		} catch (Throwable t) {
			/* ignore, because we don't want to do it in a synchronized block, so NPE can happen */
		}
	}
	
	@Override
	public void printStats(StringBuilder s) {
		try {
			s.append("Task Manager: ").append(getName()).append(" (").append(nbThreads).append(" threads):\r\n");
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
			/* ignore, because we don't want to do it in a synchronized block, so NPE can happen */
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
