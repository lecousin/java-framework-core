package net.lecousin.framework.concurrent.threads;

public interface SystemThread extends Runnable {

	/** Append a status about the thread to the given StringBuilder. */
	void debugStatus(StringBuilder s);
	
}
