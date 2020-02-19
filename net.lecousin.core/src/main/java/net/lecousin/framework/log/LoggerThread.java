package net.lecousin.framework.log;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.util.Pair;

class LoggerThread {

	@SuppressWarnings({"squid:S2142","squid:S106"})
	LoggerThread(Application app) {
		Async<Exception> stopped = new Async<>();
		thread = app.getThreadFactory().newThread(() -> {
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
		});
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
	
	private Thread thread;
	private TurnArray<Pair<Appender,Log>> logs = new TurnArray<>(200);
	private boolean stop = false;
	private Async<Exception> flushing = null;

	void log(Appender appender, Log log) {
		Pair<Appender,Log> p = new Pair<>(appender, log);
		int s = logs.size();
		synchronized (logs) {
			logs.addLast(p);
			if (s == 1)
				logs.notify();
		}
		if (s > 5000) {
			Async<Exception> a = new Async<>();
			a.block(s > 10000 ? 1000 : s > 6000 ? 500 : 100);
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
