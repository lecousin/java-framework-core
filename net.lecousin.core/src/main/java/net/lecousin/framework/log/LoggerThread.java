package net.lecousin.framework.log;

import java.io.Closeable;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.util.Pair;

class LoggerThread {

	@SuppressWarnings("resource")
	LoggerThread(Application app) {
		thread = app.getThreadFactory().newThread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Pair<Appender,Log> log;
					synchronized (logs) {
						log = logs.pollFirst();
						if (log == null) {
							if (stop) break;
							try { logs.wait(); }
							catch (InterruptedException e) { break; }
							continue;
						}
					}
					try { log.getValue1().append(log.getValue2()); }
					catch (Throwable t) {
						app.getConsole().err("Error in log appender " + log.getValue1() + ": " + t.getMessage());
						app.getConsole().err(t);
					}
				}
			}
		});
		thread.setName("Logger for " + app.getGroupId() + "." + app.getArtifactId() + " " + app.getVersion().toString());
		if (app.isStopping()) return;
		thread.start();
		app.toClose(new Closeable() {
			@Override
			public void close() {
				stop = true;
				synchronized (logs) {
					logs.notify();
				}
			}
		});
	}
	
	private Thread thread;
	private TurnArray<Pair<Appender,Log>> logs = new TurnArray<>(200);
	private boolean stop = false;

	void log(Appender appender, Log log) {
		Pair<Appender,Log> p = new Pair<>(appender,log);
		synchronized (logs) {
			logs.addLast(p);
			logs.notify();
		}
	}
	
}
