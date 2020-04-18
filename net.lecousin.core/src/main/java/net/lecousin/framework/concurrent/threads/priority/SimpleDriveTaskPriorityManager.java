package net.lecousin.framework.concurrent.threads.priority;

import java.io.File;
import java.util.LinkedList;
import java.util.Objects;

import net.lecousin.framework.concurrent.threads.DrivesThreadingManager;
import net.lecousin.framework.concurrent.threads.Task;

/** This priority manager is using the priority of tasks, then tries to first execute tasks on the same file.<br/>
 * To be able to determine on which file the task is working on, the attribute {@link DrivesThreadingManager#TASK_CONTEXT_FILE_ATTRIBUTE}
 * must be set in the task's context.<br/>
 */
public class SimpleDriveTaskPriorityManager extends SimpleTaskPriorityManager {
	
	/** Constructor. */
	@SuppressWarnings("unchecked")
	public SimpleDriveTaskPriorityManager() {
		ready = new LinkedList[Task.Priority.NB];
		for (byte i = 0; i < Task.Priority.NB; ++i)
			ready[i] = new LinkedList<>();
	}

	private File previousFile = null;

	@Override
	public Task<?, ?> peekNextOrWait() {
		Task<?, ?> task = null;
		if (previousFile != null)
			task = goThroughEligibles(t -> {
				Object f = t.getContext().getAttribute(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE);
				return Objects.equals(f, previousFile);
			});
		if (task != null)
			return task;
		task = super.peekNextOrWait();
		if (task == null)
			previousFile = null;
		else {
			Object o = task.getContext().getAttribute(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE);
			previousFile = o instanceof File ? (File)o : null;
		}
		return task;
	}
	
	@Override
	public final Task<?, ?> peekNext() {
		Task<?, ?> task = null;
		if (previousFile != null)
			task = goThroughEligibles(t -> {
				Object f = t.getContext().getAttribute(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE);
				return Objects.equals(f, previousFile);
			});
		if (task != null)
			return task;
		task = super.peekNext();
		if (task == null)
			previousFile = null;
		else {
			Object o = task.getContext().getAttribute(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE);
			previousFile = o instanceof File ? (File)o : null;
		}
		return task;
	}
	
}
