package net.lecousin.framework.concurrent.tasks.drives;

import java.io.IOException;
import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;

/**
 * Task to execute a sequence of operation on a file, and avoid separate them into different tasks.
 */
public final class DriveOperationsSequence {
	
	private DriveOperationsSequence() {
		// no instance
	}

	/** Create task. */
	public static Task<Void, IOException> create(
		TaskManager manager, String description, Priority priority,
		List<Object> resultCollector,
		@SuppressWarnings("unchecked") Executable<?, IOException>... operations
	) {
		return new Task<>(manager, description, priority, new Executable<Void, IOException>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Void execute(Task task) throws IOException, CancelException {
				for (Executable<?, IOException> op : operations) {
					if (task.isCancelling()) throw task.getCancelEvent();
					resultCollector.add(op.execute(task));
				}
				return null;
			}
		}, null);
	}
	
}
