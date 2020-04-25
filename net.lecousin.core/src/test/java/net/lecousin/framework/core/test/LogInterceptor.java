package net.lecousin.framework.core.test;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LogPattern.Log;

public class LogInterceptor implements LogFilter {

	public static final String CTX_ATTRIBUTE_FILTER = "junit.LogInterceptor";
	
	@Override
	public boolean test(Log log) {
		Task.Context ctx = Task.getCurrentContext();
		if (ctx == null)
			return false;
		try {
			LogFilter filter = (LogFilter)ctx.getAttribute(CTX_ATTRIBUTE_FILTER);
			if (filter == null)
				return false;
			return filter.test(log);
		} catch (Exception e) {
			return false;
		}
	}
	
	public static void intercept(LogFilter filter) {
		Task.Context ctx = Task.getCurrentContext();
		if (ctx != null)
			ctx.setAttribute(CTX_ATTRIBUTE_FILTER, filter);
	}
	
}
