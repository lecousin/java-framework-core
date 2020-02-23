package net.lecousin.framework.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.List;

import javax.management.NotificationEmitter;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.text.StringUtil;

/**
 * The memory manager is handling {@link IMemoryManageable} instances.
 * It also monitor memory usage and garbage collection to free additional memory when needed.
 */
public class MemoryManager {

	private MemoryManager() {}
	
	private static Logger logger;
	
	/** Initialization. */
	public static void init() {
		logger = LCCore.get().getMemoryLogger();
		if (logger.debug())
			logMemory(Level.DEBUG);

		// after 15 minutes the application is running, every 5 minutes, ask to clean expired cached data
		Task<Void,NoException> cleanExpiredData = Task.cpu("Free memory for expired cached data", Priority.BACKGROUND, () -> {
			if (logger.debug()) logger.debug("Free expired cached data");
			freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
			if (logger.debug())
				logManageableContent();
			return null;
		});
		cleanExpiredData.executeEvery(5L * 60 * 1000, 15L * 60 * 1000);
		cleanExpiredData.start();
		
		// listen to garbage collection
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			logger.debug("Garbage collector: " + gc.getName());
			MutableLong lastHeavyGCAlert = new MutableLong(0);
			((NotificationEmitter)gc).addNotificationListener((notification, handback) -> {
				long now = System.currentTimeMillis();
				if (now - gcForced < 100)
					return;
				//lastGcForcedFailed = false;
				if (now - lastGC[4] < 60000 && now - lastHeavyGCAlert.get() > 30000) {
					logger.debug("5 garbage collections in less than 1 minute");
					lastHeavyGCAlert.set(now);
				}
				System.arraycopy(lastGC, 0, lastGC, 1, 9);
				System.arraycopy(lastGCAllocatedMemory, 0, lastGCAllocatedMemory, 1, 9);
				System.arraycopy(lastGCUsedMemory, 0, lastGCUsedMemory, 1, 9);
				System.arraycopy(lastGCCollector, 0, lastGCCollector, 1, 9);
				lastGC[0] = now;
				lastGCAllocatedMemory[0] = Runtime.getRuntime().totalMemory();
				lastGCUsedMemory[0] = lastGCAllocatedMemory[0] - Runtime.getRuntime().freeMemory();
				GarbageCollectorMXBean gcBean = (GarbageCollectorMXBean)handback;
				lastGCCollector[0] = gcBean;
			}, null, gc);
		}
		
		// check memory
		Task<Void,NoException> checkMemory = Task.cpu("Check memory", Priority.BACKGROUND, MemoryManager::checkMemory);
		checkMemory.executeEvery(1L * 60 * 1000, 2L * 60 * 1000);
		checkMemory.start();

		monitorThresholds();
	}
	
	@SuppressWarnings("squid:S3776") // complexity
	private static void monitorThresholds() {
		// listen to collection usage threshold
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		((NotificationEmitter)memoryBean).addNotificationListener((notification, handback) -> {
			if (!notification.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) return;
			if (logger.info())
				logger.info("Memory threshold reached, try to free all possible cached data to free memory");
			freeMemory(FreeMemoryLevel.URGENT);
		}, null, null);
		boolean oldGenFound = false;
		int nbHeap = 0;
		for (MemoryPoolMXBean m : ManagementFactory.getMemoryPoolMXBeans()) {
			if (m.getType().equals(MemoryType.HEAP)) {
				nbHeap++;
				// we are only interested by the tenured/old generation
				if (m.getName().contains("Tenured Gen") || m.getName().contains("Old Gen")) {
					long max = m.getUsage().getMax();
					if (logger.info())
						logger.info("Monitoring memory usage: maximum = " + StringUtil.size(max));
					// set threshold to 90%
					m.setCollectionUsageThreshold(max - max / 10);
					oldGenFound = true;
				}
			}
		}
		if (!oldGenFound) {
			if (nbHeap == 1) {
				for (MemoryPoolMXBean m : ManagementFactory.getMemoryPoolMXBeans()) {
					if (m.getType().equals(MemoryType.HEAP)) {
						long max = m.getUsage().getMax();
						if (logger.info())
							logger.info("Monitoring memory usage: maximum = " + StringUtil.size(max));
						// set threshold to 90%
						m.setCollectionUsageThreshold(max - max / 10);
					}
				}
			} else {
				// here we don't know how to do
				if (logger.warn())
					logger.warn("Unable to monitor memory usage threshold");
			}
		}
	}
	
	private static long[] lastGC = new long[10];
	private static long[] lastGCUsedMemory = new long[10];
	private static long[] lastGCAllocatedMemory = new long[10];
	private static GarbageCollectorMXBean[] lastGCCollector = new GarbageCollectorMXBean[10];
	private static long gcForced = 0;
	//private static boolean lastGcForcedFailed = false;
	
	private static ArrayList<IMemoryManageable> managed = new ArrayList<>();
	
	/** Register a memory manageable instance. */
	public static void register(IMemoryManageable manageable) {
		synchronized (managed) {
			managed.add(manageable);
		}
	}

	/** Unregister a memory manageable instance. */
	public static void unregister(IMemoryManageable manageable) {
		synchronized (managed) {
			managed.remove(manageable);
		}
	}
	
	private static Void checkMemory() {
		if (logger.debug())
			logManageableContent();
		if (System.currentTimeMillis() - lastGC[0] > 120000) {
			// last garbage collection was more than 2 minutes ago
			freeMemoryBasedOnLastGarbageTime();

			if (System.currentTimeMillis() - lastGC[0] > 25 * 60 * 1000) {
				long total = Runtime.getRuntime().totalMemory();
				long allocate = total / 50; // 2%
				long free = Runtime.getRuntime().freeMemory();
				if (allocate > free / 10) allocate = free / 10;
				if (allocate > 1024 * 1024) allocate = 1024L * 1024;
				if (logger.debug() && allocate > 0)
					logger.debug("No garbage collection since 25 minutes => "
						+ "make some garbage to induce a collection sooner = " + StringUtil.size(allocate));
				while (allocate > 0) {
					int len = allocate > 4096 ? 4096 : (int)allocate;
					byte[] tmp = new byte[len];
					tmp[0] = 51;
					allocate -= len;
				}
			}

			if (logger.debug())
				logMemory(Level.DEBUG);

			/*
			if (System.currentTimeMillis() - lastGC[0] > 15 * 60 * 1000) {
				if (logger.debug()) logger.debug("No garbage collection since 15 minutes => force it");
				System.gc();
				if (logger.debug())
					logMemory(Level.DEBUG);
			}*/
		}
		return null;
	}
	
	private static void freeMemoryBasedOnLastGarbageTime() {
		if ((((System.currentTimeMillis() - lastGC[0]) / 60000) % 5) == 0) {
			// last garbage collection was more than 5 or 10 or 15... minutes ago 
			if (System.currentTimeMillis() - lastGC[4] > (20 * 60 * 1000)) {
				if (logger.debug())
					logger.debug("Less than 5 garbage collections since more than 20 minutes => "
						+ "free a maximum of memory to try to shrink the memory used by the JVM");
				freeMemory(FreeMemoryLevel.URGENT);
			} else {
				if (logger.debug())
					logger.debug("No garbage collection since 5 minutes => "
						+ "free most of cached data memory to try to shrink the memory used by the JVM");
				freeMemory(FreeMemoryLevel.MEDIUM);
			}
		} else {
			if (logger.debug())
				logger.debug("No garbage collection since 2 minutes => "
					+ "free some cached data to try to shrink the memory used by the JVM");
			freeMemory(FreeMemoryLevel.LOW);
		}
	}
	
	/** Log memory usage to the console at regular interval. */
	public static void logMemory(long interval, Level level) {
		Task<Void,NoException> task = Task.cpu("Logging memory", Priority.BACKGROUND, () -> {
			logMemory(level);
			return null;
		});
		task.executeEvery(interval, interval);
		task.start();
	}
	
	/** Log description of memory manageable instances. */
	public static void logManageableContent() {
		synchronized (managed) {
			logger.debug("Memory managed:");
			for (IMemoryManageable m : managed) {
				logger.debug(" - " + m.getDescription());
				List<String> list = m.getItemsDescription();
				if (list != null)
					for (String s : list)
						logger.debug("    - " + s);
			}
		}
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	/** Log memory usage. */
	public static void logMemory(Level level) {
		//if (jvm != null) jvm.printOptions();
		StringBuilder s = new StringBuilder(2048);
		logMemory(s);
		logger.log(level, s.toString());
	}
	
	/** Log memory status into the given StringBuilder. */
	public static void logMemory(StringBuilder s) {
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		long used = total - free;
		s.append("Memory usage: ").append(StringUtil.size(used)).append('/').append(StringUtil.size(total))
			.append(" (").append(StringUtil.size(free)).append(" free), max=")
			.append(StringUtil.size(Runtime.getRuntime().maxMemory())).append('\n');
		for (MemoryPoolMXBean m : ManagementFactory.getMemoryPoolMXBeans()) {
			s.append(" - Pool: ").append(m.getName()).append(" (").append(m.getType().name()).append("): used=")
				.append(StringUtil.size(m.getUsage().getUsed())).append(", allocated=")
				.append(StringUtil.size(m.getUsage().getCommitted())).append('/')
				.append(StringUtil.size(m.getUsage().getMax())).append(", init=")
				.append(StringUtil.size(m.getUsage().getInit())).append('\n');
		}
		s.append("Last 10 garbage collections:\n");
		for (int i = 0; i < 10; ++i) {
			if (lastGC[i] == 0) break;
			s.append(" - ")
			.append(System.currentTimeMillis() - lastGC[i])
			.append("ms ago = ")
			.append(StringUtil.size(lastGCUsedMemory[i]))
			.append(" / ")
			.append(StringUtil.size(lastGCAllocatedMemory[i]))
			.append(" [")
			.append(lastGCCollector[i].getName())
			.append(':');
			String[] pools = lastGCCollector[i].getMemoryPoolNames();
			for (int j = 0; j < pools.length; ++j) {
				if (j > 0) s.append(',');
				s.append(pools[j]);
			}
			s.append("]\n");
		}
	}
	
	/** Free memory on memory manageable instances. */
	public static void freeMemory(FreeMemoryLevel level) {
		ArrayList<IMemoryManageable> list;
		synchronized (managed) {
			list = new ArrayList<>(managed);
		}
		for (IMemoryManageable m : list) {
			if (logger.debug()) logger.debug("Free memory level " + level.name() + " on " + m.getDescription());
			try { m.freeMemory(level); }
			catch (Exception t) {
				logger.error("Error freeing memory from " + m.getDescription(), t);
			}
		}
	}
	
	/*
		 * Pools for sun:
 - Pool: Code Cache (NON_HEAP): used=4278464, allocated=4325376/251658240
 - Pool: Metaspace (NON_HEAP): used=16236440, allocated=17170432/-1
 - Pool: Compressed Class Space (NON_HEAP): used=1985504, allocated=2228224/1073741824
 - Pool: PS Eden Space (HEAP): used=11955208, allocated=34078720/697303040
 - Pool: PS Survivor Space (HEAP): used=5236416, allocated=5242880/5242880
 - Pool: PS Old Gen (HEAP): used=59696, allocated=89653248/1416626176
 
 
 
 SerialGC:
---------
Memory managers:
  CodeCacheManager [Code Cache]
  Copy [Eden Space, Survivor Space]
  MarkSweepCompact [Eden Space, Survivor Space, Tenured Gen, Perm Gen]
Memory Pools:
  Code Cache 48
  Eden Space 53
  Survivor Space 6
  Tenured Gen 133
  Perm Gen 82

ParallelGC:
-----------
Memory managers:
  CodeCacheManager [Code Cache]
  PS Scavenge [PS Eden Space, PS Survivor Space]
  PS MarkSweep [PS Eden Space, PS Survivor Space, PS Old Gen, PS Perm Gen]
Memory Pools:
  Code Cache 48
  PS Eden Space 56
  PS Survivor Space 5
  PS Old Gen 133
  PS Perm Gen 82

ParallelOldGC:
---------------
Memory managers:
  CodeCacheManager [Code Cache]
  PS Scavenge [PS Eden Space, PS Survivor Space]
  PS MarkSweep [PS Eden Space, PS Survivor Space, PS Old Gen, PS Perm Gen]
Memory Pools:
  Code Cache 48
  PS Eden Space 56
  PS Survivor Space 5
  PS Old Gen 133
  PS Perm Gen 82

ConcMarkSweepGC:
----------------
Memory managers:
  CodeCacheManager [Code Cache]
  ParNew [Par Eden Space, Par Survivor Space]
  ConcurrentMarkSweep [Par Eden Space, Par Survivor Space, CMS Old Gen, CMS Perm Gen]
Memory Pools:
  Code Cache 48
  Par Eden Space 20
  Par Survivor Space 2
  CMS Old Gen 175
  CMS Perm Gen 82

G1GC:
-----
Memory managers:
  CodeCacheManager [Code Cache]
  G1 Young Generation [G1 Eden, G1 Survivor]
  G1 Old Generation [G1 Eden, G1 Survivor, G1 Old Gen, G1 Perm Gen]
Memory Pools:
  Code Cache 48
  G1 Eden 0
  G1 Survivor 0
  G1 Old Gen 0
  G1 Perm Gen 82
  
  
IBM J9 JVM:
 - Pool: class storage (NON_HEAP)
 - Pool: JIT code cache (NON_HEAP)
 - Pool: JIT data cache (NON_HEAP)
 - Pool: miscellaneous non-heap storage (NON_HEAP)
 - Pool: Java heap (HEAP)
 
 
		 */
	
}
