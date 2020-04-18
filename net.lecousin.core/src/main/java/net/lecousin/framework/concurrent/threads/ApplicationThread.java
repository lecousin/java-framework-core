package net.lecousin.framework.concurrent.threads;

import net.lecousin.framework.application.Application;

public interface ApplicationThread extends Runnable {

	/** Return the application the thread belongs to. */
	Application getApplication();
	
	/** Append a status about the thread to the given StringBuilder. */
	void debugStatus(StringBuilder s);
	
}
