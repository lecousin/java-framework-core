package net.lecousin.framework.xml.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
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
	AsyncSupplier<?, SerializationException> deserialize(XMLDeserializer deserializer, XMLStreamEventsAsync xml, List<SerializationRule> rules);
	
	/** Serialization. */
	IAsync<SerializationException> serialize(
		Object value, XMLSerializer serializer, XMLWriter output, List<SerializationRule> rules);
}
