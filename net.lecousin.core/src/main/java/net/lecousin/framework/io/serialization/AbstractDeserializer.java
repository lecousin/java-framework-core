package net.lecousin.framework.io.serialization;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.CustomSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/**
 * Implement a Deserializer by creating a task that will execute a synchronous method.
 * @param <In> type of expected input
 */
public abstract class AbstractDeserializer<In> implements Deserializer {

	@Override
	public <T> AsyncWork<T,Exception> deserialize(
		Class<T> type, ParameterizedType ptype, IO.Readable input, byte priority,
		List<SerializationRule> rules, List<CustomSerializer<?,?>> customSerializers
	) {
		Task.Cpu<T, Exception> task = new Task.Cpu<T, Exception>("Deserialization: " + getClass().getName(), priority) {
			@Override
			public T run() throws Exception, CancelException {
				return deserialize(type, ptype, adaptInput(input), rules, customSerializers);
			}
		};
		task.start();
		return task.getSynch();
	}
	
	/** Deserialize a type from the given input. */
	public abstract <T> T deserialize(
		Class<T> type, ParameterizedType ptype, In input, List<SerializationRule> rules, List<CustomSerializer<?,?>> customSerializers
	) throws Exception;
	
	protected abstract In adaptInput(IO.Readable input);

}
