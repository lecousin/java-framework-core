package net.lecousin.framework.concurrent.threads.fixed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.TaskExecutor;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.TaskManagerMonitor;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;
import net.lecousin.framework.exception.NoException;

/**
 * Base class to implement a TaskManager, which is responsible to execute tasks in threads.
 */
public abstract class FixedThreadTaskManager extends TaskManager {
	
	/** Constructor. */
	public FixedThreadTaskManager(
		String name, Object resource, int nbThreads, ThreadFactory threadFactory, TaskPriorityManager taskPriorityManager,
		TaskManagerMonitor.Configuration monitorConfig
	) {
		super(name, resource, threadFactory, taskPriorityManager, monitorConfig);
		this.nbThreads = nbThreads;
		spare = new TurnArray<>(nbThreads * 2);
	}
	
	private int nbThreads;
	private TurnArray<TaskWorker> spare;
	private LinkedList<AsyncSupplier<TaskWorker,NoException>> pausesToDo = new LinkedList<>();
	
	public int getNbThreads() { return nbThreads; }
	
	@Override
	protected void threadingStarted() {
		Task.cpu("Close old spare threads for " + getName(), Task.Priority.BACKGROUND, new CloseOldSpare())
		.executeEvery(60000, 6L * 60000).start();
	}
	
	@Override
	protected final void finishAndStopActiveAndInactiveExecutors() {
		// stop workers
		finishAndStopWorkers();
		// stop spares
		synchronized (spare) {
			while (!spare.isEmpty())
				spare.removeFirst().forceStop(true);
		}
		// resume blocked threads
		do {
			AsyncSupplier<TaskWorker,NoException> waitPause = getPauseToDo();
			if (waitPause == null)
				break;
			waitPause.unblockSuccess(null);
		} while (true);
	}
	
	protected abstract void finishAndStopWorkers();
	
	@Override
	protected void forceStopActiveAndInactiveExecutors() {
		forceStopWorkers();
		synchronized (spare) {
			while (!spare.isEmpty())
				spare.removeFirst().forceStop(true);
		}
	}
	
	protected abstract void forceStopWorkers();
	
	@Override
	protected void executorUncaughtException(TaskExecutor executor) {
		replaceBySpare((TaskWorker)executor);
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
	
	AsyncSupplier<TaskWorker,NoException> getPauseToDo() {
		if (pausesToDo.isEmpty()) return null;
		synchronized (pausesToDo) {
			if (pausesToDo.isEmpty()) return null;
			return pausesToDo.removeFirst();
		}
	}
	
	private void replaceBySpare(TaskWorker worker) {
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
			w.getThread().start();
		} else {
			// replace the worker
			replaceWorkerBySpare(worker, w);
			// wake up this spare
			synchronized (w) { w.notify(); }
		}
	}
	
	@Override
	protected void replaceBlockedExecutor(TaskExecutor executor) {
		// a thread is blocked, we need to launch a spare
		replaceBySpare((TaskWorker)executor);
	}
	
	@Override
	protected void unblockedExecutor(TaskExecutor executor) {
		// pause the next worker as soon as it is done with its work
		AsyncSupplier<TaskWorker,NoException> pause = new AsyncSupplier<>();
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
			replaceWorkerBySpare(pause.getResult(), (TaskWorker)executor);
			synchronized (spare) {
				spare.addFirst(pause.getResult());
			}
		}
	}
	
	@Override
	public List<TaskExecutor> getActiveExecutors() {
		return Arrays.asList(getWorkers());
	}
	
	@Override
	public List<TaskExecutor> getInactiveExecutors() {
		synchronized (spare) {
			return new ArrayList<>(spare);
		}
	}
	
	@Override
	protected void executorAside(TaskExecutor executor) {
		TaskWorker newWorker = createWorker();
		replaceWorkerBySpare((TaskWorker)executor, newWorker);
		newWorker.getThread().start();
	}
	
	@Override
	protected void getDebugDescription(StringBuilder s) {
		s.append("Task Manager: ").append(getName()).append(" (").append(nbThreads).append(" threads)");
	}
	
	private class CloseOldSpare implements Executable<Void, NoException> {
		@Override
		public Void execute(Task<Void, NoException> taskContext) {
			synchronized (spare) {
				if (spare.size() <= nbThreads) return null;
				int maxToStop = (spare.size() - nbThreads) / 3 + 1;
				int nbStop = 0;
				for (Iterator<TaskWorker> it = spare.iterator(); it.hasNext(); ) {
					TaskWorker t = it.next();
					if (t.lastUsed > 5 * 60000) {
						Threading.getLogger().info("Spare thread not used since more than 5 minutes => stop it: "
							+ t.getThread().getName());
						t.forceStop(true);
						spare.removeInstance(t);
						if (++nbStop >= maxToStop)
							return null;
					}
				}
			}
			return null;
		}
	}
	
}
