package net.lecousin.framework.xml.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLWriter;

/** Custom serialization. */
public interface XMLCustomSerializer {
	
	/** Type that this serializer is handling. */
	TypeDefinition type();

	/** Deserialization. */
	AsyncWork<?, SerializationException> deserialize(XMLDeserializer deserializer, XMLStreamEventsAsync xml, List<SerializationRule> rules);
	
	/** Serialization. */
	ISynchronizationPoint<SerializationException> serialize(
		Object value, XMLSerializer serializer, XMLWriter output, List<SerializationRule> rules);
}
