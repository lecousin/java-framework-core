package net.lecousin.framework.concurrent;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.util.ThreadUtil;

/** Simple implementation of TaskPriorityManager for a single application, only using the priority of tasks. */
public class StandaloneTaskPriorityManager implements TaskPriorityManager {
	
	/** Constructor. */
	@SuppressWarnings("unchecked")
	public StandaloneTaskPriorityManager() {
		ready = new TurnArray[Task.NB_PRIORITES];
		for (byte i = 0; i < Task.NB_PRIORITES; ++i)
			ready[i] = new TurnArray<>(64);
	}

	private TurnArray<Task<?,?>>[] ready;
	private byte nextPriority = Task.NB_PRIORITES; 
	private long lastIdle = -1;
	private TaskManager taskManager;
	
	@Override
	public final void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	@Override
	public final synchronized void add(Task<?, ?> task) {
		ready[task.priority].addLast(task);
		if (nextPriority > task.priority) nextPriority = task.priority;
		this.notify();
	}
	
	@Override
	public final synchronized boolean remove(Task<?, ?> task) {
		return ready[task.priority].removeInstance(task);
	}
	
	@Override
	@SuppressWarnings({
		"squid:S2273", // wait in a loop
		"squid:S3776" // complexity
	})
	public final Task<?, ?> peekNextOrWait() {
		Task<?,?> t;
		do {
			if (nextPriority < Task.PRIORITY_BACKGROUND) {
				t = ready[nextPriority].pollFirst();
				if (t == null) {
					nextPriority++;
					continue;
				}
				if (nextPriority < Task.PRIORITY_BACKGROUND && t.executeEvery <= 0)
					lastIdle = -1;
				break;
			}
			if (nextPriority == Task.NB_PRIORITES) {
				if (lastIdle < 0)
					lastIdle = System.currentTimeMillis();
				ThreadUtil.wait(this, 0);
				return null;
			}
			if (nextPriority == Task.PRIORITY_BACKGROUND) {
				// for background tasks, we wait for at least 1 second of idle
				if (lastIdle < 0)
					lastIdle = System.currentTimeMillis();
				long wait = lastIdle + 1000 - System.currentTimeMillis();
				if (wait > 50) {
					if (!taskManager.isStopping() && taskManager.getTransferTarget() == null) {
						ThreadUtil.wait(this, wait);
						return null;
					}
					return null;
				}
				t = ready[Task.PRIORITY_BACKGROUND].pollFirst();
				if (t == null) {
					nextPriority++;
					continue;
				}
				break;
			}
		} while (true);
		return t;
	}
	
	@Override
	public final Task<?, ?> peekNext() {
		Task<?,?> t;
		do {
			if (nextPriority == Task.NB_PRIORITES) {
				if (lastIdle < 0)
					lastIdle = System.currentTimeMillis();
				return null;
			}
			t = ready[nextPriority].pollFirst();
			if (t == null) {
				nextPriority++;
				continue;
			}
			if (nextPriority < Task.PRIORITY_BACKGROUND && t.executeEvery <= 0)
				lastIdle = -1;
			break;
		} while (true);
		return t;
	}
	
	@Override
	public final List<Task<?, ?>> removeAllPendingTasks() {
		LinkedList<Task<?,?>> list = new LinkedList<>();
		for (byte p = 0; p < Task.NB_PRIORITES; ++p) {
			while (!ready[p].isEmpty())
				list.add(ready[p].removeFirst());
		}
		return list;
	}
	
	@Override
	@SuppressWarnings("squid:S106") // print to console
	public final void forceStop() {
		nextPriority = Task.NB_PRIORITES;
		synchronized (ready) {
			ready.notifyAll();
		}
		nextPriority = Task.NB_PRIORITES;
		for (byte p = 0; p < Task.NB_PRIORITES; ++p) {
			if (ready[p].isEmpty()) continue;
			for (Task<?,?> t : ready[p])
				System.err.println("Task Manager " + taskManager.getName()
					+ " stopped while the following task was remaining: " + t.description);
		}
	}
	
	@Override
	public final int getRemainingTasks(boolean includingBackground) {
		int nb = 0;
		synchronized (this) {
			for (byte p = 0; p < Task.NB_PRIORITES; ++p) if (includingBackground || p != Task.PRIORITY_BACKGROUND) nb += ready[p].size();
		}
		return nb;
	}
	
	@Override
	public final boolean hasRemainingTasks(boolean includingBackground) {
		synchronized (this) {
			for (byte p = 0; p < Task.NB_PRIORITES; ++p)
				if ((includingBackground || p != Task.PRIORITY_BACKGROUND) &&
					!ready[p].isEmpty())
						return true;
			return false;
		}
	}
}
