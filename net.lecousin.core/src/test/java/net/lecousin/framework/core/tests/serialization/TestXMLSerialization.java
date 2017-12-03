package net.lecousin.framework.core.tests.serialization;

import net.lecousin.framework.core.test.serialization.TestSerialization;
import net.lecousin.framework.io.serialization.Deserializer;
import net.lecousin.framework.io.serialization.Serializer;
import net.lecousin.framework.xml.serialization.XMLDeserializer;
import net.lecousin.framework.xml.serialization.XMLSerializer;

public class TestXMLSerialization extends TestSerialization {

	@Override
	protected Serializer createSerializer() {
		return new XMLSerializer(null, "test", null);
	}
	
	@Override
	protected Deserializer createDeserializer() {
		return new XMLDeserializer(null, "test");
	}
	
}
