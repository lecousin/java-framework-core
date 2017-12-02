package net.lecousin.framework.xml.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLWriter;

/** Custom serialization.
 * @param <T> type
 */
public interface XMLCustomSerializer {

	/** Deserialization. */
	public AsyncWork<?, Exception> deserialize(XMLDeserializer deserializer, XMLStreamReaderAsync xml, List<SerializationRule> rules);
	
	/** Serialization. */
	public ISynchronizationPoint<? extends Exception> serialize(Object value, XMLSerializer serializer, XMLWriter output, List<SerializationRule> rules);
}
