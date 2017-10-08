package net.lecousin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/** TaskManager for CPU tasks, using one thread by available processor. */
public class CPUTaskManager extends MultiThreadTaskManager {

	CPUTaskManager(ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager) {
		super("CPU", Threading.CPU, Runtime.getRuntime().availableProcessors(), threadFactory, taskPriorityManager);
	}
	
}
