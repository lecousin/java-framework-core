package net.lecousin.framework.concurrent;

import java.util.List;

/**
 * Interface to implement to manage the priority of tasks.
 * Task workers (threads) will come to peek the next task to execute.
 */
public interface TaskPriorityManager {

	/** Set the associated TaskManager that will execute tasks. */
	void setTaskManager(TaskManager taskManager);
	
	/** Add a task to execute. */
	void add(Task<?,?> task);
	
	/** Remove a task. */
	boolean remove(Task<?,?> task);
	
	/** Return a task if one is immediately available, or pause the calling thread.
	 * If the thread is paused, null is returned when resumed in order to give a chance
	 * to check if there is something more urgent than peeking a new task. 
	 * This method is always called inside a synchronized block on this instance.
	 */
	Task<?,?> peekNextOrWait();
	
	/** Return the number of waiting tasks. */
	int getRemainingTasks(boolean includingBackground);
	
	/** Return true if at least one task is waiting to execute. */
	boolean hasRemainingTasks(boolean includingBackground);
	
	/** always called inside a synchronized block on this instance. */
	List<Task<?,?>> removeAllPendingTasks();
	
	/** always called inside a synchronized block on this instance. */
	void forceStop();
	
}
