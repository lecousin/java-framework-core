package net.lecousin.framework.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.collections.map.MapUtil;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.util.Pair;

/** Handle TaskManagers for a drives. */
public class DrivesTaskManager {

	/** Constructor. */
	public DrivesTaskManager(
		ThreadFactory threadFactory,
		Class<? extends TaskPriorityManager> taskPriorityManager,
		DrivesProvider drivesProvider
	) {
		this.threadFactory = threadFactory;
		this.taskPriorityManager = taskPriorityManager;
		rootResources = new HashMap<>();
		rootManagers = new HashMap<>();
		managers = new HashMap<>();
		if (drivesProvider == null) {
			// by default, one resource per root
			for (File root : File.listRoots()) {
				String path = root.getAbsolutePath();
				if (path.charAt(path.length() - 1) != File.separatorChar)
					path += File.separatorChar;
				Object resource = new Object();
				MonoThreadTaskManager tm = new MonoThreadTaskManager("Drive " + path, resource, threadFactory, taskPriorityManager);
				tm.start();
				rootResources.put(path, resource);
				rootManagers.put(path, tm);
				managers.put(resource, tm);
				Threading.registerResource(resource, tm);
			}
		} else
			setDrivesProvider(drivesProvider);
	}
	
	private ThreadFactory threadFactory;
	private Class<? extends TaskPriorityManager> taskPriorityManager;
	private Map<String,Object> rootResources;
	private Map<String,MonoThreadTaskManager> rootManagers;
	private Map<Object,MonoThreadTaskManager> managers;
	
	/** Return the associated resource for the given file. */
	public Object getResource(File file) {
		return getResource(file.getAbsolutePath());
	}
	
	/** Return the associated resource for the given file path. */
	public Object getResource(String path) {
		synchronized (rootManagers) {
			for (Map.Entry<String, Object> e : rootResources.entrySet())
				if (path.startsWith(e.getKey()))
					return e.getValue();
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
		synchronized (rootManagers) {
			for (Map.Entry<String, MonoThreadTaskManager> e : rootManagers.entrySet())
				if (path.startsWith(e.getKey()))
					return e.getValue();
		}
		return null;
	}
	
	/** Interface to provide drives and partitions. */
	public static interface DrivesProvider {
		/** Register listeners. */
		public void provide(
			Listener<Pair<Object,List<File>>> onNewDrive,
			Listener<Pair<Object,List<File>>> onDriveRemoved,
			Listener<Pair<Object,File>> onNewPartition,
			Listener<Pair<Object,File>> onPartitionRemoved
		);
	}
	
	private DrivesProvider drivesProvider = null;
	
	/** Set the drives provider.
	 * @throws IllegalStateException in case a provider is already set
	 */
	@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
	public void setDrivesProvider(DrivesProvider provider) throws IllegalStateException {
		synchronized (this) {
			if (drivesProvider != null) throw new IllegalStateException();
			drivesProvider = provider;
		}
		drivesProvider.provide(
			(d) -> { newDrive(d); },
			(d) -> { driveRemoved(d); },
			(p) -> { newPartition(p); },
			(p) -> { partitionRemoved(p); }
		);
	}
	
	private void newDrive(Pair<Object,List<File>> driveAndPartitions) {
		Object drive = driveAndPartitions.getValue1();
		MonoThreadTaskManager tm = new MonoThreadTaskManager("Drive " + drive.toString(), drive, threadFactory, taskPriorityManager);
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
			MonoThreadTaskManager previous;
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
		Threading.logger.info("Task manager added: " + name.toString());
		tm.started();
	}
	
	private void driveRemoved(Pair<Object,List<File>> driveAndPartitions) {
		Object drive = driveAndPartitions.getValue1();
		MonoThreadTaskManager tm;
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
		MonoThreadTaskManager tm;
		synchronized (managers) {
			tm = managers.get(drive);
		}
		if (tm == null) {
			Threading.logger.error("Cannot handle partition " + mount.getAbsolutePath() + " because drive is unknown: " + drive);
			return;
		}
		MonoThreadTaskManager previous;
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
		Threading.logger.info("New partition added to DrivesTaskManager: " + mount.getAbsolutePath());
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
