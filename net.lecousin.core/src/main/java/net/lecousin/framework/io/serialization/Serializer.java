package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Serializer. */
public interface Serializer {

	/** Serialize the given object. */
	ISynchronizationPoint<SerializationException> serialize(
		Object object, TypeDefinition typeDef, IO.Writable output, List<SerializationRule> rules);
	
}
