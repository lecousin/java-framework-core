package net.lecousin.framework.core.tests.serialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.core.test.serialization.TestSerialization;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.serialization.Deserializer;
import net.lecousin.framework.io.serialization.Serializer;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;
import net.lecousin.framework.xml.serialization.XMLDeserializer;
import net.lecousin.framework.xml.serialization.XMLSerializer;

@RunWith(Parameterized.class)
public class TestXMLSerialization extends TestSerialization {

	@Parameters(name = "efficient = {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[] { Boolean.TRUE }, new Object[] { Boolean.FALSE });
	}
	
	public TestXMLSerialization(boolean efficient) {
		this.efficient = efficient;
	}
	
	protected boolean efficient;
	
	@Override
	protected Serializer createSerializer() {
		if (efficient)
			return new XMLSerializer(null, "test", null);
		return new XMLSerializer(null, "test", null) {
			@SuppressWarnings("resource")
			@Override
			protected ISynchronizationPoint<IOException> initializeSerialization(Writable output) {
				bout = new SimpleBufferedWritable(output, 5);
				this.output = new XMLWriter(new BufferedWritableCharacterStream(bout, StandardCharsets.UTF_8, 3), includeXMLDeclaration);
				if (namespaces == null)
					namespaces = new HashMap<>();
				if (!namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
					namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
				return this.output.start(rootNamespaceURI, rootLocalName, namespaces);
			}
		};
	}
	
	@Override
	protected Deserializer createDeserializer() {
		if (efficient)
			return new XMLDeserializer(null, "test");
		return new XMLDeserializer(null, "test") {
			@Override
			protected ISynchronizationPoint<Exception> createAndStartReader(Readable input) {
				XMLStreamReaderAsync reader = new XMLStreamReaderAsync(input, forceEncoding, 2);
				this.input = reader;
				reader.setMaximumTextSize(16384);
				reader.setMaximumCDataSize(16384);
				return reader.startRootElement();
			}
		};
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void print(IO.Readable.Seekable io, Object o) throws Exception {
		super.print(io, o);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		// parser will close the IO, so we need to wrap it
		builder.parse(IOAsInputStream.get(new SubIO.Readable.Seekable(io, 0, ((IO.KnownSize)io).getSizeSync(), io.getSourceDescription(), false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
	}
	
}
