package net.lecousin.framework.concurrent.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.lecousin.framework.collections.map.MapUtil;
import net.lecousin.framework.concurrent.threads.fixed.MonoThreadTaskManager;
import net.lecousin.framework.concurrent.threads.fixed.MultiThreadTaskManager;
import net.lecousin.framework.concurrent.threads.priority.SimpleTaskPriorityManager;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;

/** Handle TaskManagers for a drives. */
public class DrivesThreadingManager {
	
	public static final String TASK_CONTEXT_FILE_ATTRIBUTE = "task.file";
	
	private static final String DRIVE = "Drive ";

	/** Constructor. */
	DrivesThreadingManager(
		ThreadFactory threadFactory,
		Supplier<TaskPriorityManager> taskPriorityManagerSupplier,
		DrivesProvider drivesProvider,
		TaskManagerMonitor.Configuration driveMonitoring
	) {
		this.threadFactory = threadFactory;
		this.taskPriorityManagerSupplier = taskPriorityManagerSupplier;
		this.driveMonitoring = driveMonitoring;
		rootResources = new HashMap<>();
		rootManagers = new HashMap<>();
		managers = new HashMap<>();
		if (drivesProvider == null) {
			// by default, one resource per root
			for (File root : File.listRoots()) {
				String path = root.getAbsolutePath();
				if (path.charAt(path.length() - 1) != File.separatorChar)
					path += File.separatorChar;
				Object resource = root;
				TaskPriorityManager prio;
				try {
					prio = taskPriorityManagerSupplier.get();
				} catch (Exception e) {
					Threading.getLogger().error("Unable to instantiate task priority manager", e);
					prio = new SimpleTaskPriorityManager();
				}
				TaskManager tm = new MonoThreadTaskManager(
					DRIVE + path, resource, threadFactory, prio, driveMonitoring);
				tm.start();
				rootResources.put(path, resource);
				rootManagers.put(path, tm);
				managers.put(resource, tm);
				Threading.registerResource(resource, tm);
			}
		} else {
			setDrivesProvider(drivesProvider);
		}
	}
	
	private ThreadFactory threadFactory;
	private Supplier<TaskPriorityManager> taskPriorityManagerSupplier;
	private Map<String, Object> rootResources;
	private Map<String, TaskManager> rootManagers;
	private Map<Object, TaskManager> managers;
	private TaskManagerMonitor.Configuration driveMonitoring;
	
	/** Return the associated resource for the given file. */
	public Object getResource(File file) {
		return getResource(file.getAbsolutePath());
	}
	
	/** Return the associated resource for the given file path. */
	public Object getResource(String path) {
		Map.Entry<String, Object> bestMatch = null;
		synchronized (rootManagers) {
			for (Map.Entry<String, Object> e : rootResources.entrySet())
				if (path.startsWith(e.getKey()) &&
					(bestMatch == null || bestMatch.getKey().length() < e.getKey().length()))
						bestMatch = e;
			if (bestMatch != null)
				return bestMatch.getValue();
		}
		return null;
	}
	
	/** Return the list of resources. */
	public List<Object> getResources() {
		synchronized (managers) {
			return new ArrayList<>(managers.keySet());
		}
	}
	
	/** Return the TaskManager for the given file. */
	public TaskManager getTaskManager(File file) {
		return getTaskManager(file.getAbsolutePath());
	}

	/** Return the TaskManager for the given file path. */
	public TaskManager getTaskManager(String path) {
		Map.Entry<String, TaskManager> bestMatch = null;
		synchronized (rootManagers) {
			for (Map.Entry<String, TaskManager> e : rootManagers.entrySet())
				if (path.startsWith(e.getKey()) &&
					(bestMatch == null || bestMatch.getKey().length() < e.getKey().length()))
						bestMatch = e;
			if (bestMatch != null)
				return bestMatch.getValue();
		}
		return null;
	}

	/** Return the known drive partition path for the given file. */
	public String getPartitionPath(File file) {
		return getPartitionPath(file.getAbsolutePath());
	}
	
	/** Return the known drive partition path for the given file path. */
	public String getPartitionPath(String path) {
		Map.Entry<String, TaskManager> bestMatch = null;
		synchronized (rootManagers) {
			for (Map.Entry<String, TaskManager> e : rootManagers.entrySet())
				if (path.startsWith(e.getKey()) &&
					(bestMatch == null || bestMatch.getKey().length() < e.getKey().length()))
						bestMatch = e;
			if (bestMatch != null)
				return bestMatch.getKey();
		}
		return null;
	}
	
	void setMonitoringConfiguration(TaskManagerMonitor.Configuration config) {
		synchronized (rootManagers) {
			driveMonitoring = config;
			for (TaskManager tm : rootManagers.values())
				tm.getMonitor().setConfiguration(config);
		}
	}
	
	/** Interface to provide drives and partitions. */
	public static interface DrivesProvider {
		/** Register listeners. */
		void provide(
			Consumer<Triple<Object, List<File>, Boolean>> onNewDrive,
			Consumer<Object> onDriveRemoved,
			Consumer<Pair<Object, File>> onNewPartition,
			Consumer<Pair<Object, File>> onPartitionRemoved
		);
	}
	
	private DrivesProvider drivesProvider = null;
	
	/** Set the drives provider.
	 * @throws IllegalStateException in case a provider is already set
	 */
	public void setDrivesProvider(DrivesProvider provider) {
		synchronized (this) {
			if (drivesProvider != null) throw new IllegalStateException();
			drivesProvider = provider;
		}
		drivesProvider.provide(
			this::newDrive,
			this::driveRemoved,
			this::newPartition,
			this::partitionRemoved
		);
	}
	
	private void newDrive(Triple<Object, List<File>, Boolean> driveAndPartitions) {
		Object drive = driveAndPartitions.getValue1();
		boolean multiThread = driveAndPartitions.getValue3() != null && driveAndPartitions.getValue3().booleanValue();
		TaskPriorityManager prio;
		try {
			prio = taskPriorityManagerSupplier.get();
		} catch (Exception e) {
			Threading.getLogger().error("Unable to instantiate task priority manager", e);
			prio = new SimpleTaskPriorityManager();
		}
		TaskManager tm;
		if (multiThread)
			tm = new MultiThreadTaskManager(
				DRIVE + drive.toString(), drive, 2, threadFactory, prio, driveMonitoring);
		else
			tm = new MonoThreadTaskManager(
				DRIVE + drive.toString(), drive, threadFactory, prio, driveMonitoring);
		tm.start();
		synchronized (managers) {
			managers.put(drive, tm);
		}
		Threading.registerResource(drive, tm);
		
		List<File> partitions = driveAndPartitions.getValue2();
		if (partitions == null || partitions.isEmpty()) return;
		ArrayList<String> paths = new ArrayList<>();
		for (File mount : partitions) {
			String path = mount.getAbsolutePath();
			if (path.charAt(path.length() - 1) != File.separatorChar)
				path += File.separatorChar;
			paths.add(path);
			TaskManager previous;
			synchronized (rootManagers) {
				previous = rootManagers.get(path);
				if (previous != null) {
					Threading.unregisterResource(rootResources.get(path));
					rootManagers.remove(path);
					rootResources.remove(path);
					rootManagers.put(path, tm);
					rootResources.put(path, drive);
				} else {
					rootManagers.put(path, tm);
					rootResources.put(path, drive);
				}
			}
			if (previous != null)
				previous.transferAndClose(tm);
		}
		StringBuilder name = new StringBuilder();
		name.append(tm.getName()).append(" (");
		for (int i = 0; i < paths.size(); ++i) {
			if (i > 0) name.append(" + ");
			name.append(paths.get(i));
		}
		name.append(')');
		tm.setName(name.toString());
		Threading.getLogger().info("Task manager added: " + name.toString());
		tm.started();
	}
	
	private void driveRemoved(Object drive) {
		TaskManager tm;
		synchronized (managers) {
			tm = managers.remove(drive);
		}
		if (tm != null) {
			Threading.unregisterResource(drive);
			synchronized (rootManagers) {
				MapUtil.removeValue(rootManagers, tm);
				MapUtil.removeValue(rootResources, drive);
			}
			tm.cancelAndStop();
		}
	}
	
	private void newPartition(Pair<Object,File> driveAndPartition) {
		Object drive = driveAndPartition.getValue1();
		File mount = driveAndPartition.getValue2();
		String path = mount.getAbsolutePath();
		if (path.charAt(path.length() - 1) != File.separatorChar)
			path += File.separatorChar;
		TaskManager tm;
		synchronized (managers) {
			tm = managers.get(drive);
		}
		if (tm == null) {
			Threading.getLogger().error("Cannot handle partition " + mount.getAbsolutePath() + " because drive is unknown: " + drive);
			return;
		}
		TaskManager previous;
		synchronized (rootManagers) {
			previous = rootManagers.get(path);
			if (previous != null) {
				rootManagers.remove(path);
				rootResources.remove(path);
				rootManagers.put(path, tm);
				Object prevResource = rootResources.put(path, drive);
				if (!rootResources.containsValue(prevResource))
					Threading.unregisterResource(prevResource);
			} else {
				rootManagers.put(path, tm);
				rootResources.put(path, drive);
			}
		}
		if (previous != null && previous != tm)
			previous.transferAndClose(tm);
		Threading.getLogger().info("New partition added to DrivesTaskManager: " + mount.getAbsolutePath());
	}
	
	private void partitionRemoved(Pair<Object,File> driveAndPartition) {
		String path = driveAndPartition.getValue2().getAbsolutePath();
		if (path.charAt(path.length() - 1) != File.separatorChar)
			path += File.separatorChar;
		synchronized (rootManagers) {
			Threading.unregisterResource(rootResources.get(path));
			rootManagers.remove(path);
			rootResources.remove(path);
		}
	}
	
}
