package net.lecousin.framework.concurrent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.application.libraries.Library;
import net.lecousin.framework.collections.map.MapUtil;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.mutable.Mutable;

/** Handle TaskManagers for a drives. */
public class DrivesTaskManager {

	/** Constructor. */
	public DrivesTaskManager(ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager) {
		this.threadFactory = threadFactory;
		this.taskPriorityManager = taskPriorityManager;
		rootResources = new HashMap<>();
		rootManagers = new HashMap<>();
		managers = new HashMap<>();
		// first, one resource per root
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
		// then improve
		Mutable<ClassLoader> clLoader = new Mutable<>(DrivesTaskManager.class.getClassLoader());
		Task.Cpu<Void,NoException> improve = new Task.Cpu<Void,NoException>("Loading Drives information", Task.PRIORITY_RATHER_IMPORTANT) {
			@Override
			public Void run() {
				System.out.println("Loading drives information to improve multi-threading");
				Class<?> cl;
				try { cl = Class.forName("net.lecousin.framework.system.hardware.Drives", true, clLoader.get()); }
				catch (ClassNotFoundException e) {
					System.out.println(
						"Library net.lecousin.framework.system.impl is missing: threading on hard drives cannot be optimized"
					);
					return null;
				}
				try {
					Class<?> clListener = Class.forName(
						"net.lecousin.framework.system.hardware.Drives$DriveListener", true, clLoader.get());
					Object drives = cl.getField("instance").get(null);
					if (drives == null)
						throw new Exception("Drives instance not initialized");
					Method m = cl.getMethod("getDrivesAndListen", clListener);
					Class<?> clListenerImpl = Class.forName(
						"net.lecousin.framework.system.hardware.Drives$DriveListenerImpl", true, clLoader.get());
					@SuppressWarnings("rawtypes")
					Object listener = clListenerImpl.getConstructor(
							Listener.class, Listener.class, Listener.class, Listener.class
					).newInstance(new Listener() {
						@Override
						public void fire(Object event) {
							System.out.println("Add task manager for new drive " + event);
							newDrive(event);
						}
					}, new Listener() {
						@Override
						public void fire(Object event) {
							System.out.println("Remove task manager for new drive " + event);
							driveRemoved(event);
						}
					}, new Listener() {
						@Override
						public void fire(Object event) {
							// TODO new partition (event is DiskPartition)
							
						}
					}, new Listener() {
						@Override
						public void fire(Object event) {
							// TODO partition removed (event is DiskPartition)
							
						}
					});
					m.invoke(drives, listener);
					m = cl.getMethod("initialize");
					m.invoke(drives);
				} catch (Throwable t) {
					System.err.println("Error loading drives information");
					t.printStackTrace(System.err);
				}
				return null;
			}
		};
		LibrariesManager libs = LCCore.get().getSystemLibraries();
		libs.onLibrariesLoaded().listenInline(new Runnable() {
			@Override
			public void run() {
				if (!libs.canLoadNewLibraries()) {
					improve.start();
					return;
				}
				// TODO get the version in another way
				AsyncWork<Library,Exception> t = libs.loadNewLibrary(
					"net.lecousin.framework", "system.impl", new VersionSpecification.SingleVersion(new Version("0.2")),
					true, Task.PRIORITY_RATHER_IMPORTANT, null, 0);
				t.listenInline(new Runnable() {
					@Override
					public void run() {
						if (t.getResult() == null) {
							if (t.getError() != null)
								Threading.logger.error("Error loading library net.lecousin.framework.system.impl",
									t.getError());
							else
								Threading.logger.warn(
					"Unable to load library net.lecousin.framework.system.impl: threading on hard drives cannot be optimized"
								);
							return;
						}
						clLoader.set(t.getResult().getClassLoader());
						improve.start();
					}
				});
			}
		});
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void newDrive(Object drive) {
		MonoThreadTaskManager tm = new MonoThreadTaskManager("Drive " + drive.toString(), drive, threadFactory, taskPriorityManager);
		tm.start();
		synchronized (managers) {
			managers.put(drive, tm);
		}
		Threading.registerResource(drive, tm);
		List<File> mountPoints = null;
		Class<?> cl = null;
		try { cl = Class.forName("net.lecousin.framework.system.hardware.PhysicalDrive"); }
		catch (Throwable t) { /* ignore */ }
		if (cl != null && cl.isAssignableFrom(drive.getClass())) {
			List partitions = null;
			try { partitions = (List)drive.getClass().getMethod("getPartitions").invoke(drive); }
			catch (Throwable t) {
				Threading.logger.error("Error loading partitions from drive " + drive.toString(), t);
			}
			if (partitions != null) {
				mountPoints = new LinkedList<>();
				for (Object partition : partitions) {
					try {
						File mount = (File)partition.getClass().getMethod("getMountPoint").invoke(partition);
						if (mount == null) continue;
						mountPoints.add(mount);
					} catch (Throwable t) {
						Threading.logger.error("Error searching mount points for partition on drive " + drive.toString(), t);
					}
				}
			}
		} else {
			try { mountPoints = (List<File>)drive.getClass().getMethod("getMountPoints").invoke(drive); }
			catch (Throwable t) {
				Threading.logger.error("Error loading mount points from drive " + drive.toString(), t);
			}
		}
		
		if (mountPoints == null || mountPoints.isEmpty()) return;
		ArrayList<String> paths = new ArrayList<>();
		for (File mount : mountPoints) {
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
		tm.autoCloseSpares();
	}
	
	private void driveRemoved(Object drive) {
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
	
}
