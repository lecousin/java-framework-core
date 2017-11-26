package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public interface SerializationSpecification {

	<T> AsyncWork<T, Exception> writeSpecification(Class<T> type, boolean nullable, IO.Writable ouput, List<SerializationRule> rules);
	
}
