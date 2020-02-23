package net.lecousin.framework.concurrent.threads.priority;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.util.ThreadUtil;

/** Simple implementation of TaskPriorityManager only using the priority of tasks.
 * This is typically used in a single application environment on resources that does not need
 * specific order to improve performance.
 */
public class SimpleTaskPriorityManager implements TaskPriorityManager {
	
	/** Constructor. */
	@SuppressWarnings("unchecked")
	public SimpleTaskPriorityManager() {
		ready = new TurnArray[Task.Priority.NB];
		for (byte i = 0; i < Task.Priority.NB; ++i)
			ready[i] = new TurnArray<>(64);
		background = new TurnArray<>(32);
	}

	private TurnArray<Task<?,?>>[] ready;
	private TurnArray<Task<?,?>> background;
	private int nextPriority = Task.Priority.NB; 
	private long lastIdle = -1;
	private boolean stopping = false;

	@Override
	public final synchronized void add(Task<?, ?> task) {
		int p = task.getPriority().getValue();
		if (p == Task.Priority.BACKGROUND.getValue()) {
			background.add(task);
		} else {
			ready[p].addLast(task);
			if (nextPriority > p) nextPriority = p;
		}
		this.notify();
	}
	
	@Override
	public final synchronized boolean remove(Task<?, ?> task) {
		int p = task.getPriority().getValue();
		if (p == Task.Priority.BACKGROUND.getValue())
			return background.removeInstance(task);
		return ready[p].removeInstance(task);
	}
	
	@Override
	@SuppressWarnings({
		"squid:S2273", // wait in a loop
		"squid:S3776" // complexity
	})
	public final Task<?, ?> peekNextOrWait() {
		Task<?,?> t;
		do {
			if (nextPriority < Task.Priority.NB) {
				t = ready[nextPriority].pollFirst();
				if (t == null) {
					nextPriority++;
					continue;
				}
				if (t.getRepetitionDelay() <= 0)
					lastIdle = -1;
				break;
			}
			// for background tasks, we wait for at least 1 second of idle
			if (lastIdle < 0)
				lastIdle = System.currentTimeMillis();
			long wait = lastIdle + 1000 - System.currentTimeMillis();
			if (wait > 50) {
				if (!stopping)
					ThreadUtil.wait(this, wait);
				return null;
			}
			t = background.pollFirst();
			if (t == null) {
				if (!stopping)
					ThreadUtil.wait(this, 0);
				return null;
			}
			break;
		} while (true);
		return t;
	}
	
	@Override
	public final Task<?, ?> peekNext() {
		Task<?,?> t;
		do {
			if (nextPriority < Task.Priority.NB) {
				t = ready[nextPriority].pollFirst();
				if (t == null) {
					nextPriority++;
					continue;
				}
				if (t.getRepetitionDelay() <= 0)
					lastIdle = -1;
				break;
			}
			if (lastIdle < 0)
				lastIdle = System.currentTimeMillis();
			return null;
		} while (true);
		return t;
	}
	
	@Override
	public void stopping() {
		stopping = true;
	}
	
	@Override
	public final List<Task<?, ?>> removeAllPendingTasks() {
		LinkedList<Task<?,?>> list = new LinkedList<>();
		for (int p = 0; p < ready.length; ++p) {
			while (!ready[p].isEmpty())
				list.add(ready[p].removeFirst());
		}
		while (!background.isEmpty())
			list.add(background.removeFirst());
		return list;
	}
	
	@Override
	@SuppressWarnings("squid:S106") // print to console
	public final void forceStop() {
		nextPriority = Task.Priority.NB;
		synchronized (ready) {
			ready.notifyAll();
		}
		nextPriority = Task.Priority.NB;
		for (int p = 0; p < Task.Priority.NB; ++p) {
			if (ready[p].isEmpty()) continue;
			for (Task<?,?> t : ready[p])
				System.err.println("Task Manager stopped while the following task was remaining: " + t.getDescription());
		}
	}
	
	@Override
	public final int getRemainingTasks(boolean includingBackground) {
		int nb = 0;
		synchronized (this) {
			for (int p = 0; p < Task.Priority.NB; ++p) nb += ready[p].size();
			if (includingBackground)
				nb += background.size();
		}
		return nb;
	}
	
	@Override
	public final boolean hasRemainingTasks(boolean includingBackground) {
		synchronized (this) {
			if (includingBackground && !background.isEmpty())
				return false;
			for (int p = 0; p < Task.Priority.NB; ++p)
				if (!ready[p].isEmpty())
					return true;
			return false;
		}
	}
}
