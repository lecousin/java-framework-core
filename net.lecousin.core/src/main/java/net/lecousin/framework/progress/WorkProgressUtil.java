package net.lecousin.framework.progress;

/** Utility methods. */
public class WorkProgressUtil {

	/** Propagate the progression of a sub-task to a prent. */
	public static void propagateToParent(WorkProgress subTask, WorkProgress parentTask, long amount) {
		Runnable update = new Runnable() {
			private long propagated = 0;
			@Override
			public void run() {
				long pos = subTask.getAmount();
				if (pos > 0) pos = subTask.getPosition() * amount / pos;
				if (pos == propagated) return;
				synchronized (parentTask) {
					parentTask.progress(pos - propagated);
				}
				propagated = pos;
			}
		};
		subTask.listen(update);
		update.run();
	}
	
}
