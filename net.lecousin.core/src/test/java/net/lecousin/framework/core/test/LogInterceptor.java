package net.lecousin.framework.core.test;

import java.util.LinkedList;
import java.util.List;

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
			@SuppressWarnings("unchecked")
			List<LogFilter> filters = (List<LogFilter>)ctx.getAttribute(CTX_ATTRIBUTE_FILTER);
			if (filters == null)
				return false;
			for (LogFilter filter : filters)
				if (filter.test(log))
					return true;
		} catch (Exception e) {
			// ignore
		}
		return false;
	}
	
	public static List<LogFilter> getFilters() {
		Task.Context ctx = Task.getCurrentContext();
		if (ctx == null)
			return new LinkedList<>();
		@SuppressWarnings("unchecked")
		List<LogFilter> filters = (List<LogFilter>)ctx.getAttribute(CTX_ATTRIBUTE_FILTER);
		if (filters == null) {
			filters = new LinkedList<>();
			ctx.setAttribute(CTX_ATTRIBUTE_FILTER, filters);
		}
		return filters;
	}
	
}
