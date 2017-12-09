package net.lecousin.framework.xml.serialization;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.serialization.AbstractSerializationSpecWriter;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;

public class XMLSpecWriter extends AbstractSerializationSpecWriter {

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		this(rootNamespaceURI, rootLocalName, namespaces, StandardCharsets.UTF_8, true);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, StandardCharsets.UTF_8, includeXMLDeclaration);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, true);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, includeXMLDeclaration);
	}

	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, bufferSize, true);
	}
	
	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize, boolean includeXMLDeclaration) {
		this.rootNamespaceURI = rootNamespaceURI;
		this.rootLocalName = rootLocalName;
		this.namespaces = namespaces;
		this.encoding = encoding;
		this.bufferSize = bufferSize;
		this.includeXMLDeclaration = includeXMLDeclaration;
	}
	
	protected String rootNamespaceURI;
	protected String rootLocalName;
	protected Map<String, String> namespaces;
	protected Charset encoding;
	protected int bufferSize;
	protected boolean includeXMLDeclaration;
	protected IO.Writable.Buffered bout;
	protected XMLWriter output;
	
	@Override
	protected ISynchronizationPoint<IOException> initializeSpecWriter(IO.Writable output) {
		if (output instanceof IO.Writable.Buffered)
			bout = (IO.Writable.Buffered)output;
		else
			bout = new SimpleBufferedWritable(output, bufferSize);
		this.output = new XMLWriter(bout, encoding, includeXMLDeclaration);
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
		output.addAttribute("type", "xsd:boolean");
		if (nullable)
			output.addAttribute("nillable", "true");
		return output.closeElement();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> specifyNumericValue(Class<?> type, boolean nullable, Number min, Number max) {
		if (byte.class.equals(type) ||
			Byte.class.equals(type))
			output.addAttribute("type", "xsd:byte");
		else if (int.class.equals(type) ||
			Integer.class.equals(type))
			output.addAttribute("type", "xsd:int");
		else if (long.class.equals(type) ||
			Long.class.equals(type))
			output.addAttribute("type", "xsd:long");
		else if (short.class.equals(type) ||
			Short.class.equals(type) ||
			BigInteger.class.equals(type)
		)
			output.addAttribute("type", "xsd:integer");
		else
			output.addAttribute("type", "xsd:decimal");
		if (nullable)
			output.addAttribute("nillable", "true");
		// TODO min, max ? defining a type ??
		return output.closeElement();
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> specifyStringValue(SerializationContext context, TypeDefinition type) {
		output.addAttribute("type", "xsd:string");
		// TODO restrictions ?
		return output.closeElement();
	}
	
	public static String getTypeName(Class<?> type) {
		return type.getName().replace('$', '-');
	}
	
	protected static class TypeContext {
		public boolean sequenceStarted = false;
	}
	
	protected LinkedList<TypeContext> typesContext = new LinkedList<>();

	@Override
	protected ISynchronizationPoint<? extends Exception> specifyAnyValue(SerializationContext context) {
		output.endOfAttributes();
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "complexType", null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "sequence", null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "any", null);
		output.addAttribute("minOccurs", "0");
		output.closeElement();
		output.closeElement();
		output.closeElement();
		return output.closeElement();
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> specifyTypedValue(ObjectContext context, List<SerializationRule> rules) {
		output.endOfAttributes();
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "complexType", null);
		TypeContext ctx = new TypeContext();
		typesContext.addFirst(ctx);
		ISynchronizationPoint<? extends Exception> type = specifyTypeContent(context, rules);
		if (type.isUnblocked()) {
			if (type.hasError()) return type;
			typesContext.removeFirst();
			if (ctx.sequenceStarted) output.closeElement();
			output.closeElement(); // complexType
			return output.closeElement(); // value
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		type.listenAsyncSP(new SpecTask(() -> {
			typesContext.removeFirst();
			if (ctx.sequenceStarted) output.closeElement();
			output.closeElement(); // complexType
			output.closeElement().listenInlineSP(sp); // value
		}), sp);
		return sp;
	}

	protected static final boolean isAttribute(Class<?> c) {
		if (c.isPrimitive()) return true;
		if (Boolean.class.equals(c)) return true;
		if (Number.class.isAssignableFrom(c)) return true;
		if (String.class.equals(c)) return true;
		return false;
	}
	
	protected static final Comparator<Attribute> attributesComparator = new Comparator<Attribute>() {
		@Override
		public int compare(Attribute o1, Attribute o2) {
			Class<?> c = o1.getType().getBase();
			if (isAttribute(c)) return 1;
			c = o2.getType().getBase();
			if (isAttribute(c)) return -1;
			return 0;
		}
	};
	
	@Override
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		attributes.sort(attributesComparator);
		return attributes;
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> specifyTypeAttribute(AttributeContext context, List<SerializationRule> rules) {
		Attribute a = context.getAttribute();
		Class<?> type = a.getType().getBase();
		TypeContext ctx = typesContext.getFirst();
		if (isAttribute(type)) {
			if (ctx.sequenceStarted) {
				output.closeElement();
				ctx.sequenceStarted = false;
			}
			output.openElement(XMLUtil.XSD_NAMESPACE_URI, "attribute", null);
			output.addAttribute("name", a.getName());
			return specifyValue(context, a.getType(), rules);
		}
		if (!ctx.sequenceStarted) {
			output.openElement(XMLUtil.XSD_NAMESPACE_URI, "sequence", null);
			ctx.sequenceStarted = true;
		}
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "element", null);
		output.addAttribute("name", a.getName());
		return specifyValue(context, a.getType(), rules);
	}
	
}
