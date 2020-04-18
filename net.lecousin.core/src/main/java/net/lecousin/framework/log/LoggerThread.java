package net.lecousin.framework.log;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.ApplicationThread;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.util.Pair;

class LoggerThread {

	LoggerThread(Application app) {
		this.app = app;
		thread = app.createThread(new Worker());
		thread.setName("Logger for " + app.getGroupId() + "." + app.getArtifactId() + " " + app.getVersion().toString());
		if (app.isStopping()) return;
		thread.start();
		app.toClose(100001, () -> {
			stop = true;
			synchronized (logs) {
				logs.notify();
			}
			return stopped;
		});
	}
	
	private Application app;
	private Thread thread;
	Async<Exception> stopped = new Async<>();
	private TurnArray<Pair<Appender,Log>> logs = new TurnArray<>(200);
	private boolean stop = false;
	private Async<Exception> flushing = null;
	
	private class Worker implements ApplicationThread {
		@Override
		public Application getApplication() {
			return app;
		}
		
		@Override
		@SuppressWarnings({"squid:S2142","squid:S106"})
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
		
		@Override
		public void debugStatus(StringBuilder s) {
			s.append(" - Logging: ").append(logs.size()).append(" pending logs\r\n");
		}
	}

	@SuppressWarnings({"java:S3358", "java:S2142"})
	void log(Appender appender, Log log) {
		Pair<Appender,Log> p = new Pair<>(appender, log);
		synchronized (logs) {
			logs.addLast(p);
			logs.notify();
		}
		int s = logs.size();
		if (s > 5000) {
			// pause the thread to let time to logging
			try {
				Thread.sleep(s > 10000 ? 250 : s > 7500 ? 100 : 50);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	IAsync<Exception> flush() {
		synchronized (logs) {
			if (flushing != null) return flushing;
			flushing = new Async<>();
			logs.notify();
			return flushing;
		}
	}
	
}
