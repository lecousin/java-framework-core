package net.lecousin.framework.concurrent.threads.priority;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

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
			ready[i] = new TurnArray<>(128);
		background = new TurnArray<>(32);
	}

	protected Deque<Task<?,?>>[] ready;
	protected Deque<Task<?,?>> background;
	protected int nextPriority = Task.Priority.NB; 
	protected long lastIdle = -1;
	protected boolean stopping = false;

	@Override
	public final synchronized void add(Task<?, ?> task) {
		int p = task.getPriority().getValue();
		if (p == Task.BACKGROUND_VALUE) {
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
		if (p == Task.BACKGROUND_VALUE)
			return background.remove(task);
		return ready[p].remove(task);
	}
	
	@Override
	@SuppressWarnings({
		"squid:S2273", // wait in a loop
		"squid:S3776" // complexity
	})
	public Task<?, ?> peekNextOrWait() {
		while (nextPriority < Task.Priority.NB) {
			Task<?,?> t = ready[nextPriority].pollFirst();
			if (t != null) {
				if (t.getRepetitionDelay() <= 0)
					lastIdle = -1;
				return t;
			}
			nextPriority++;
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
		Task<?,?> t = background.pollFirst();
		if (t != null)
			return t;
		if (!stopping)
			ThreadUtil.wait(this, 0);
		return null;
	}
	
	@Override
	public Task<?, ?> peekNext() {
		while (nextPriority < Task.Priority.NB) {
			Task<?,?> t = ready[nextPriority].pollFirst();
			if (t != null) {
				if (t.getRepetitionDelay() <= 0)
					lastIdle = -1;
				return t;
			}
			nextPriority++;
		}
		if (lastIdle < 0)
			lastIdle = System.currentTimeMillis();
		return null;
	}
	
	protected Task<?, ?> goThroughEligibles(Predicate<Task<?, ?>> chooser) {
		for (int p = nextPriority; p < Task.Priority.NB; p++) {
			for (Iterator<Task<?, ?>> it = ready[p].iterator(); it.hasNext(); ) {
				Task<?, ?> t = it.next();
				if (chooser.test(t)) {
					it.remove();
					return t;
				}
			}
			if (!ready[p].isEmpty())
				return null;
		}
		return null;
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
