package net.lecousin.framework.io.buffering;

import java.io.IOException;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.buffering.BufferingManager.Buffer;
import net.lecousin.framework.util.ConcurrentCloseable;

/** Managed instances by BufferingManager. */
public abstract class BufferingManaged extends ConcurrentCloseable {

	abstract AsyncWork<?,IOException> flushWrite(Buffer buffer);
	
	abstract boolean removing(Buffer buffer);

	boolean closing = false;
	
	abstract ISynchronizationPoint<Exception> closed();
	
}
