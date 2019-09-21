package net.lecousin.framework.log;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.Pair;

class LoggerThread {

	LoggerThread(Application app) {
		SynchronizationPoint<Exception> stopped = new SynchronizationPoint<>();
		thread = app.getThreadFactory().newThread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Pair<Appender,Log> log;
					synchronized (logs) {
						log = logs.pollFirst();
						if (log == null) {
							if (flushing != null) {
								flushing.unblock();
								flushing = null;
							}
							if (stop) break;
							try { logs.wait(); }
							catch (InterruptedException e) { break; }
							continue;
						}
					}
					try { log.getValue1().append(log.getValue2()); }
					catch (Exception t) {
						app.getConsole().err("Error in log appender " + log.getValue1() + ": " + t.getMessage());
						app.getConsole().err(t);
					}
				}
				System.out.println("Logger Thread stopped.");
				stopped.unblock();
			}
		});
		thread.setName("Logger for " + app.getGroupId() + "." + app.getArtifactId() + " " + app.getVersion().toString());
		if (app.isStopping()) return;
		thread.start();
		app.toClose(new AsyncCloseable<Exception>() {
			@Override
			public ISynchronizationPoint<Exception> closeAsync() {
				stop = true;
				synchronized (logs) {
					logs.notify();
				}
				return stopped;
			}
		});
	}
	
	private Thread thread;
	private TurnArray<Pair<Appender,Log>> logs = new TurnArray<>(200);
	private boolean stop = false;
	private SynchronizationPoint<Exception> flushing = null;

	void log(Appender appender, Log log) {
		Pair<Appender,Log> p = new Pair<>(appender,log);
		synchronized (logs) {
			logs.addLast(p);
			logs.notify();
		}
	}
	
	ISynchronizationPoint<Exception> flush() {
		synchronized (logs) {
			if (flushing != null) return flushing;
			flushing = new SynchronizationPoint<>();
			logs.notify();
			return flushing;
		}
	}
	
}
