package net.lecousin.framework.core.test;

import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LogPattern;

public class TestExceptionLogFilter implements LogFilter {

	@Override
	public boolean test(LogPattern.Log log) {
		if (log.trace == null)
			return false;
		Throwable t = log.trace;
		do {
			if (t instanceof TestException)
				return true;
			Throwable cause = t.getCause();
			if (cause == null || cause == t)
				return false;
			t = cause;
		} while (true);
	}
	
}
