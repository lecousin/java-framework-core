package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Write the specification for (de)serialization. */
public interface SerializationSpecWriter {

	/** Write the specification for the given type. */
	ISynchronizationPoint<Exception> writeSpecification(Class<?> type, IO.Writable output, List<SerializationRule> rules);
	
}
