package net.lecousin.framework.core.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.text.StringUtil;
import net.lecousin.framework.text.pattern.StringPatternFilter;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TraceOnTestFailure extends TestWatcher {
	
	@Override
	protected void starting(Description description) {
		if (!description.isTest())
			return;
		Task.Context ctx = Task.getCurrentContext();
		if (ctx == null)
			return;
		ctx.setAttribute(TraceOnTestFailure.class.getName(), new Logs());
	}
	
	@Override
	protected void failed(Throwable e, Description description) {
		if (!description.isTest())
			return;
		Task.Context ctx = Task.getCurrentContext();
		if (ctx == null)
			return;
		Logs logs = (Logs)ctx.getAttribute(TraceOnTestFailure.class.getName());
		if (logs != null)
			logs.trace(description);
	}
	
	@Override
	protected void finished(Description description) {
		if (!description.isTest())
			return;
		Task.Context ctx = Task.getCurrentContext();
		if (ctx == null)
			return;
		Logs logs = (Logs)ctx.removeAttribute(TraceOnTestFailure.class.getName());
		if (logs != null)
			logs.clean();
	}
	
	private static class Logs {
		private LogFilter filter;
		private List<Log> logs;
		
		private Logs() {
			logs = new LinkedArrayList<>(100);
			filter = log -> {
				if (log.context == null || !Logs.this.equals(log.context.getAttribute(TraceOnTestFailure.class.getName())))
					return false;
				logs.add(log);
				if (log.level.ordinal() < Level.INFO.ordinal())
					return true;
				return false;
			};
			LCCore.getApplication().getLoggerFactory().addFilter(filter);
		}
		
		private void clean() {
			logs = null;
			LCCore.getApplication().getLoggerFactory().removeFilter(filter);
		}
		
		private void trace(Description description) {
			Console c = LCCore.getApplication().getConsole();
			c.out(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			c.out("Test failed: " + description.getClassName() + " " + description.getMethodName());
			c.out("Traces:");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm::ss.SSS");
			StringPatternFilter.Filter loggerNameFilter = new StringPatternFilter.FixedLength().build("..30");
			for (Log log : logs) {
				StringBuilder s = new StringBuilder();
				s.append(dateFormat.format(new Date(log.timestamp)));
				s.append(' ');
				StringUtil.paddingRight(s, log.level.name(), 6, ' ');
				s.append(loggerNameFilter.filter(log.loggerName));
				s.append(' ');
				s.append(log.message);
				c.out(s.toString());
				if (log.trace != null)
					c.out(log.trace);
			}
			c.out("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
	}

}
