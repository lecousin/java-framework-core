package net.lecousin.framework.core.tests.serialization;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.core.test.serialization.TestSerialization;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.MemoryIO;
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
	
	@SuppressWarnings("resource")
	@Override
	protected void print(MemoryIO io, Object o) throws Exception {
		super.print(io, o);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		// parser will close the IO, so we need to wrap it
		builder.parse(IOAsInputStream.get(new SubIO.Readable.Seekable.Buffered(io, 0, io.getSizeSync(), io.getSourceDescription(), false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
	}
	
}
