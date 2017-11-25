package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public interface Deserializer {

	<T> AsyncWork<T, Exception> deserialize(Class<T> type, IO.Readable input, List<SerializationRule> rules);
	
}
