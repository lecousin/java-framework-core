package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Serialize an object to a writable IO. */
public interface Serializer {

	/** Serialize an object to a writable IO. */
	public AsyncWork<Void,Exception> serialize(Object obj, List<SerializationRule> rules, IO.Writable output, byte priority);
	
}
