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
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.serialization.AbstractSerializationSpecWriter;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;

/** Write an XSD corresponding to what would be serialized in XML. */
public class XMLSpecWriter extends AbstractSerializationSpecWriter {

	/** Constructor. */
	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		this(rootNamespaceURI, rootLocalName, namespaces, StandardCharsets.UTF_8, true);
	}

	/** Constructor. */
	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, StandardCharsets.UTF_8, includeXMLDeclaration);
	}

	/** Constructor. */
	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, true);
	}

	/** Constructor. */
	public XMLSpecWriter(
		String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces,
		Charset encoding, boolean includeXMLDeclaration
	) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, includeXMLDeclaration);
	}

	/** Constructor. */
	public XMLSpecWriter(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, bufferSize, true);
	}
	
	/** Constructor. */
	public XMLSpecWriter(
		String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces,
		Charset encoding, int bufferSize, boolean includeXMLDeclaration
	) {
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
	protected boolean pretty;
	
	public void setPretty(boolean pretty) {
		this.pretty = pretty;
	}
	
	private static Function<IOException, SerializationException> ioErrorConverter =
		e -> new SerializationException("Error writing XSD", e);

	private static final String TYPE = "type";
	private static final String TYPE_BOOLEAN = "xsd:boolean";
	private static final String TYPE_BYTE = "xsd:byte";
	private static final String TYPE_INT = "xsd:int";
	private static final String TYPE_INTEGER = "xsd:integer";
	private static final String TYPE_LONG = "xsd:long";
	private static final String TYPE_FLOAT = "xsd:float";
	private static final String TYPE_DOUBLE = "xsd:double";
	private static final String TYPE_DECIMAL = "xsd:decimal";
	private static final String TYPE_STRING = "xsd:string";
	
	private static final String NILLABLE = "nillable";
	private static final String TRUE = "true";
	private static final String USE = "use";
	private static final String OPTIONAL = "optional";
	
	private static final String MIN_OCCURS = "minOccurs";
	private static final String MAX_OCCURS = "maxOccurs";
	private static final String UNBOUNDED = "unbounded";
	
	private static final String COMPLEX_TYPE = "complexType";
	private static final String SEQUENCE = "sequence";
	private static final String ELEMENT = "element";
	private static final String ATTRIBUTE = "attribute";
	
	@Override
	protected IAsync<SerializationException> initializeSpecWriter(IO.Writable output) {
		if (output instanceof IO.Writable.Buffered)
			bout = (IO.Writable.Buffered)output;
		else
			bout = new SimpleBufferedWritable(output, bufferSize);
		this.output = new XMLWriter(bout, encoding, includeXMLDeclaration, pretty);
		if (this.namespaces == null) this.namespaces = new HashMap<>();
		if (!this.namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
			this.namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
		if (!this.namespaces.containsKey(XMLUtil.XSD_NAMESPACE_URI))
			this.namespaces.put(XMLUtil.XSD_NAMESPACE_URI, "xsd");
		this.output.start(XMLUtil.XSD_NAMESPACE_URI, "schema", namespaces);
		if (rootNamespaceURI != null)
			this.output.addAttribute("targetNamespace", rootNamespaceURI);
		this.output.openElement(XMLUtil.XSD_NAMESPACE_URI, ELEMENT, null);
		return new Async<>(this.output.addAttribute("name", rootLocalName), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> finalizeSpecWriter() {
		Async<SerializationException> sp = new Async<>();
		output.end().onDone(() -> bout.flush().onDone(sp, ioErrorConverter), sp, ioErrorConverter);
		return sp;
	}
	
	@Override
	protected IAsync<SerializationException> specifyBooleanValue(SerializationContext context, boolean nullable) {
		output.addAttribute(TYPE, TYPE_BOOLEAN);
		if (nullable) {
			if (context instanceof AttributeContext)
				output.addAttribute(USE, OPTIONAL);
			else
				output.addAttribute(NILLABLE, TRUE);
		}
		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyNumericValue(
		SerializationContext context, Class<?> type, boolean nullable, Number min, Number max
	) {
		if (byte.class.equals(type) ||
			Byte.class.equals(type))
			output.addAttribute(TYPE, TYPE_BYTE);
		else if (int.class.equals(type) ||
			Integer.class.equals(type))
			output.addAttribute(TYPE, TYPE_INT);
		else if (long.class.equals(type) ||
			Long.class.equals(type))
			output.addAttribute(TYPE, TYPE_LONG);
		else if (short.class.equals(type) ||
			Short.class.equals(type) ||
			BigInteger.class.equals(type)
		)
			output.addAttribute(TYPE, TYPE_INTEGER);
		else if (float.class.equals(type) ||
				Float.class.equals(type))
			output.addAttribute(TYPE, TYPE_FLOAT);
		else if (double.class.equals(type) ||
				Double.class.equals(type))
			output.addAttribute(TYPE, TYPE_DOUBLE);
		else
			output.addAttribute(TYPE, TYPE_DECIMAL);
		
		if (nullable) {
			if (context instanceof AttributeContext)
				output.addAttribute(USE, OPTIONAL);
			else
				output.addAttribute(NILLABLE, TRUE);
		}

		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		// TODO min, max ? defining a type ??
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyStringValue(SerializationContext context, TypeDefinition type) {
		output.addAttribute(TYPE, TYPE_STRING);
		if (context instanceof AttributeContext)
			output.addAttribute(USE, OPTIONAL);
		else
			output.addAttribute(NILLABLE, TRUE);
		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		// TODO restrictions ?
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyCharacterValue(SerializationContext context, boolean nullable) {
		if (nullable) {
			if (context instanceof AttributeContext)
				output.addAttribute(USE, OPTIONAL);
			else
				output.addAttribute(NILLABLE, TRUE);
		}
		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "simpleType", null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "restriction", null);
		output.addAttribute("base", TYPE_STRING);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "length", null);
		output.addAttribute("value", "1");
		output.closeElement(); // length
		output.closeElement(); // restriction
		output.closeElement(); // simpleType
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyEnumValue(SerializationContext context, TypeDefinition type) {
		if (context instanceof AttributeContext)
			output.addAttribute(USE, OPTIONAL);
		else
			output.addAttribute(NILLABLE, TRUE);
		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "simpleType", null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "restriction", null);
		output.addAttribute("base", TYPE_STRING);
		try {
			Enum<?>[] values = (Enum<?>[])type.getBase().getMethod("values").invoke(null);
			for (int i = 0; i < values.length; ++i) {
				output.openElement(XMLUtil.XSD_NAMESPACE_URI, "enumeration", null);
				output.addAttribute("value", values[i].name());
				output.closeElement();
			}
		} catch (Exception t) {
			/* should not happen */
		}
		output.closeElement(); // restriction
		output.closeElement(); // simpleType
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyCollectionValue(CollectionContext context, List<SerializationRule> rules) {
		if (context.getParent() instanceof AttributeContext)
			return specifyValue(context, context.getElementType(), rules);
		output.addAttribute(NILLABLE, TRUE);
		if (context.getParent() instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		output.endOfAttributes();
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, COMPLEX_TYPE, null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, SEQUENCE, null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, ELEMENT, null);
		output.addAttribute("name", ELEMENT);
		IAsync<SerializationException> val = specifyValue(context, context.getElementType(), rules);
		if (val.isDone()) {
			if (val.hasError()) return val;
			output.closeElement(); // sequence
			output.closeElement(); // complexType
			return new Async<>(output.closeElement(), ioErrorConverter); // collection
		}
		Async<SerializationException> sp = new Async<>();
		val.thenStart(new SpecTask(() -> {
			output.closeElement(); // sequence
			output.closeElement(); // complexType
			output.closeElement().onDone(sp, ioErrorConverter); // collection
		}), sp);
		return sp;
	}
	
	@Override
	protected IAsync<SerializationException> specifyIOReadableValue(SerializationContext context, List<SerializationRule> rules) {
		output.addAttribute(TYPE, "xsd:base64Binary");
		output.addAttribute(NILLABLE, TRUE);
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	/** Return the name for a type, replacing dollars by minus character. */
	public static String getTypeName(Class<?> type) {
		return type.getName().replace('$', '-');
	}
	
	private static class TypeContext {
		private boolean sequenceStarted = false;
	}
	
	private LinkedList<TypeContext> typesContext = new LinkedList<>();

	@Override
	protected IAsync<SerializationException> specifyAnyValue(SerializationContext context) {
		if (context instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		output.endOfAttributes();
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, COMPLEX_TYPE, null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, SEQUENCE, null);
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, "any", null);
		output.addAttribute(MIN_OCCURS, "0");
		output.closeElement();
		output.closeElement();
		output.closeElement();
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> specifyTypedValue(ObjectContext context, List<SerializationRule> rules) {
		if (context.getParent() instanceof CollectionContext) {
			output.addAttribute(MIN_OCCURS, "0");
			output.addAttribute(MAX_OCCURS, UNBOUNDED);
		}
		output.addAttribute(NILLABLE, TRUE);
		output.endOfAttributes();
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, COMPLEX_TYPE, null);
		TypeContext ctx = new TypeContext();
		typesContext.addFirst(ctx);
		IAsync<SerializationException> type = specifyTypeContent(context, rules);
		if (type.isDone()) {
			if (type.hasError()) return type;
			typesContext.removeFirst();
			if (ctx.sequenceStarted) output.closeElement();
			output.closeElement(); // complexType
			return new Async<>(output.closeElement(), ioErrorConverter); // value
		}
		Async<SerializationException> sp = new Async<>();
		type.thenStart(new SpecTask(() -> {
			typesContext.removeFirst();
			if (ctx.sequenceStarted) output.closeElement();
			output.closeElement(); // complexType
			output.closeElement().onDone(sp, ioErrorConverter); // value
		}), sp);
		return sp;
	}

	protected static final boolean isAttribute(Class<?> c) {
		return
			c.isPrimitive() ||
			Boolean.class.equals(c) ||
			Number.class.isAssignableFrom(c) ||
			CharSequence.class.isAssignableFrom(c) ||
			Character.class.equals(c) ||
			c.isEnum();
	}
	
	protected static final Comparator<Attribute> attributesComparator = (o1, o2) -> {
		Class<?> c = o1.getType().getBase();
		if (isAttribute(c)) return 1;
		c = o2.getType().getBase();
		if (isAttribute(c)) return -1;
		return 0;
	};
	
	@Override
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		attributes.sort(attributesComparator);
		return attributes;
	}
	
	@Override
	protected IAsync<SerializationException> specifyTypeAttribute(AttributeContext context, List<SerializationRule> rules) {
		Attribute a = context.getAttribute();
		Class<?> type = a.getType().getBase();
		TypeContext ctx = typesContext.getFirst();
		if (isAttribute(type)) {
			if (ctx.sequenceStarted) {
				output.closeElement();
				ctx.sequenceStarted = false;
			}
			output.openElement(XMLUtil.XSD_NAMESPACE_URI, ATTRIBUTE, null);
			output.addAttribute("name", a.getName());
			return specifyValue(context, a.getType(), rules);
		}
		if (!ctx.sequenceStarted) {
			output.openElement(XMLUtil.XSD_NAMESPACE_URI, SEQUENCE, null);
			ctx.sequenceStarted = true;
		}
		output.openElement(XMLUtil.XSD_NAMESPACE_URI, ELEMENT, null);
		output.addAttribute("name", a.getName());
		return specifyValue(context, a.getType(), rules);
	}
	
}
