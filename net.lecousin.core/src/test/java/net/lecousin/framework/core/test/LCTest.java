package net.lecousin.framework.core.test;

import net.lecousin.framework.LCCoreVersion;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.io.provider.TestURIProvider;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.mutable.MutableBoolean;

public class LCTest {

	public static synchronized void init() throws Exception {
		if (System.getProperty(Application.PROPERTY_LOGGING_CONFIGURATION_URL) == null)
			System.setProperty(Application.PROPERTY_LOGGING_CONFIGURATION_URL, "classpath:lc-test-logging.xml");
		Application.start(new Artifact("net.lecousin.framework.test", "test", new Version(LCCoreVersion.VERSION)), true).blockThrow(0);
		LCCore.getApplication().getLoggerFactory().getLogger(Async.class).setLevel(Logger.Level.INFO);
		Threading.setLogThreadingInterval(120000);
		MemoryManager.logMemory(120000, Level.DEBUG);
		TestURIProvider.getInstance(); // make sure it is registered
	}
	
	public static synchronized void close() {
		MutableBoolean stopped = LCCore.stop(false);
		synchronized (stopped) {
			try {
				while (!stopped.get())
					stopped.wait();
			} catch (InterruptedException e) {
				// stop
			}
		}
	}
}
