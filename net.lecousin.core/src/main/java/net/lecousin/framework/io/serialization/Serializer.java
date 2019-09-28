package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Serializer. */
public interface Serializer {

	/** Serialize the given object. */
	IAsync<SerializationException> serialize(
		Object object, TypeDefinition typeDef, IO.Writable output, List<SerializationRule> rules);
	
}
