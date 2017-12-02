package net.lecousin.framework.xml.serialization;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.serialization.AbstractSerializationSpecWriter;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;

public class XMLSpecWriter extends AbstractSerializationSpecWriter {

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		this(rootNamespaceURI, rootLocalName, namespaces, StandardCharsets.UTF_8);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize) {
		this.rootNamespaceURI = rootNamespaceURI;
		this.rootLocalName = rootLocalName;
		this.namespaces = namespaces;
		this.encoding = encoding;
		this.bufferSize = bufferSize;
	}
	
	protected String rootNamespaceURI;
	protected String rootLocalName;
	protected Map<String, String> namespaces;
	protected Charset encoding;
	protected int bufferSize;
	protected IO.Writable.Buffered bout;
	protected XMLWriter output;
	
	@Override
	protected ISynchronizationPoint<IOException> initializeSpecWriter(IO.Writable output) {
		if (output instanceof IO.Writable.Buffered)
			bout = (IO.Writable.Buffered)output;
		else
			bout = new SimpleBufferedWritable(output, bufferSize);
		this.output = new XMLWriter(bout, encoding, true);
		if (this.namespaces == null) this.namespaces = new HashMap<>();
		if (!this.namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
			this.namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
		if (!this.namespaces.containsKey(XMLUtil.XSD_NAMESPACE_URI))
			this.namespaces.put(XMLUtil.XSD_NAMESPACE_URI, "xsd");
		this.output.start(XMLUtil.XSD_NAMESPACE_URI, "schema", namespaces);
		this.output.addAttribute("targetNamespace", rootNamespaceURI);
		this.output.openElement(XMLUtil.XSD_NAMESPACE_URI, "element", null);
		return this.output.addAttribute("name", rootLocalName);
	}
	
	@Override
	protected ISynchronizationPoint<IOException> finalizeSpecWriter() {
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		output.end().listenInline(() -> { bout.flush().listenInline(sp); }, sp);
		return sp;
	}
	
	@Override
	protected ISynchronizationPoint<IOException> specifyBooleanValue(boolean nullable) {
		if (!nullable)
			return output.addAttribute("type", "xsd:boolean");
		output.addAttribute("type", "xsd:boolean");
		return output.addAttribute("nillable", "true");
	}
	
	@Override
	protected ISynchronizationPoint<IOException> specifyNumericValue(Class<?> type, boolean nullable, Number min, Number max) {
		ISynchronizationPoint<IOException> res;
		if (byte.class.equals(type) ||
			Byte.class.equals(type))
			res = output.addAttribute("type", "xsd:byte");
		else if (int.class.equals(type) ||
			Integer.class.equals(type))
			res = output.addAttribute("type", "xsd:int");
		else if (long.class.equals(type) ||
			Long.class.equals(type))
			res = output.addAttribute("type", "xsd:long");
		else if (short.class.equals(type) ||
			Short.class.equals(type) ||
			BigInteger.class.equals(type)
		)
			res = output.addAttribute("type", "xsd:integer");
		else
			res = output.addAttribute("type", "xsd:decimal");
		if (nullable)
			res = output.addAttribute("nillable", "true");
		// TODO min, max ? defining a type ??
		return res;
	}
	
}
