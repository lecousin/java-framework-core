package net.lecousin.framework.io.serialization;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Deserialize a type from a readable IO. */
public interface Deserializer {

	/** Deserialize a type from a readable IO. */
	public <T> AsyncWork<T,Exception> deserialize(
		Class<T> type, ParameterizedType ptype, IO.Readable input, byte priority, List<SerializationRule> rules
	);
	
}
