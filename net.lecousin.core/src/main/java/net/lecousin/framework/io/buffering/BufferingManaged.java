package net.lecousin.framework.io.buffering;

import java.io.IOException;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO.AbstractIO;
import net.lecousin.framework.io.buffering.BufferingManager.Buffer;

/** Managed instances by BufferingManager. */
public abstract class BufferingManaged extends AbstractIO {

	abstract AsyncWork<?,IOException> flushWrite(Buffer buffer);
	
	abstract void removed(Buffer buffer);
	
	abstract ISynchronizationPoint<IOException> closed();
	
}
