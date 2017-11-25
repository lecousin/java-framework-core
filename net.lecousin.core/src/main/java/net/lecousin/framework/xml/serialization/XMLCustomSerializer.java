package net.lecousin.framework.xml.serialization;

import java.util.List;
import java.util.Set;

import net.lecousin.framework.io.serialization.rules.CustomSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

/** Custom serialization.
 * @param <T> type
 */
public interface XMLCustomSerializer<T> {

	/** Deserialization. */
	public T deserialize(
		XMLDeserializer deserializer, XMLStreamReaderAsync xml,
		List<SerializationRule> rules, List<CustomSerializer<?,?>> customSerializers
	) throws Exception;
	
	/** Serialization. */
	public void serialize(
		T value, XMLSerializer serializer, ICharacterStream.Writable.Buffered output,
		List<SerializationRule> rules, List<CustomSerializer<?, ?>> customSerializers, Set<Object> alreadySerialized
	) throws Exception;
}
