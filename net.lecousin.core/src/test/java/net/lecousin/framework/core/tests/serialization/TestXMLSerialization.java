package net.lecousin.framework.core.tests.serialization;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.core.test.serialization.TestSerialization;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.SubIO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.serialization.Deserializer;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.SerializationSpecWriter;
import net.lecousin.framework.io.serialization.Serializer;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;
import net.lecousin.framework.xml.serialization.XMLDeserializer;
import net.lecousin.framework.xml.serialization.XMLSerializer;
import net.lecousin.framework.xml.serialization.XMLSpecWriter;

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
			protected ISynchronizationPoint<SerializationException> initializeSerialization(Writable output) {
				bout = new SimpleBufferedWritable(output, 5);
				this.output = new XMLWriter(new BufferedWritableCharacterStream(bout, StandardCharsets.UTF_8, 3), includeXMLDeclaration, false);
				if (namespaces == null)
					namespaces = new HashMap<>();
				if (!namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
					namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
				return this.output.start(rootNamespaceURI, rootLocalName, namespaces).convertSP(e -> new SerializationException("Error writing XML", e));
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
				XMLStreamReaderAsync reader = new XMLStreamReaderAsync(input, forceEncoding, 2, 32);
				this.input = reader;
				reader.setMaximumTextSize(16384);
				reader.setMaximumCDataSize(16384);
				return reader.startRootElement();
			}
		};
	}
	
	@Override
	protected SerializationSpecWriter createSpecWriter() {
		if (efficient)
			return new XMLSpecWriter(null, "test", null);
		return new XMLSpecWriter(null, "test", null, null, 4, true);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void print(IO.Readable.Seekable io, Object o) throws Exception {
		super.print(io, o);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		// parser will close the IO, so we need to wrap it
		builder.parse(IOAsInputStream.get(new SubIO.Readable.Seekable(io, 0, ((IO.KnownSize)io).getSizeSync(), io.getSourceDescription(), false), false));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void checkSpec(IO.Readable.Seekable spec, Class<?> type, IO.Readable.Seekable serialization) throws Exception {
		// parser will close the IO, so we need to wrap it
		Schema schema = SchemaFactory.newInstance(XMLUtil.XSD_NAMESPACE_URI)
			.newSchema(new StreamSource(IOAsInputStream.get(new SubIO.Readable.Seekable(spec, 0, ((IO.KnownSize)spec).getSizeSync(), spec.getSourceDescription(), false), false)));
		spec.seekSync(SeekType.FROM_BEGINNING, 0);
		
		Validator validator = schema.newValidator();
		serialization.seekSync(SeekType.FROM_BEGINNING, 0);
        validator.validate(new StreamSource(IOAsInputStream.get(new SubIO.Readable.Seekable(serialization, 0, ((IO.KnownSize)serialization).getSizeSync(), serialization.getSourceDescription(), false), false)));
		serialization.seekSync(SeekType.FROM_BEGINNING, 0);
	}
	
}
