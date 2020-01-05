package net.lecousin.framework.xml.serialization;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.encoding.Base64Encoding;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.io.serialization.AbstractSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringIso8859;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;

/** Serialization into XML. */
public class XMLSerializer extends AbstractSerializer {

	/** Constructor. */
	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		this(rootNamespaceURI, rootLocalName, namespaces, null);
	}

	/** Constructor. */
	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, null, includeXMLDeclaration);
	}

	/** Constructor. */
	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096);
	}

	/** Constructor. */
	public XMLSerializer(
		String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces,
		Charset encoding, boolean includeXMLDeclaration
	) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, includeXMLDeclaration);
	}

	/** Constructor. */
	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, bufferSize, true);
	}

	/** Constructor. */
	public XMLSerializer(
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
	
	/** Constructor. */
	public XMLSerializer(XMLWriter writer) {
		this.output = writer;
	}
	
	protected String rootNamespaceURI;
	protected String rootLocalName;
	protected Map<String, String> namespaces;
	protected Charset encoding;
	protected int bufferSize;
	protected boolean includeXMLDeclaration;
	protected IO.Writable.Buffered bout;
	protected XMLWriter output;
	protected boolean pretty = false;
	
	public void setPretty(boolean pretty) {
		this.pretty = pretty;
	}
	
	private static Function<IOException, SerializationException> ioErrorConverter =
		e -> new SerializationException("Error writing XML", e);
	
	@Override
	protected IAsync<SerializationException> initializeSerialization(IO.Writable output) {
		if (output instanceof IO.Writable.Buffered)
			bout = (IO.Writable.Buffered)output;
		else
			bout = new SimpleBufferedWritable(output, bufferSize);
		this.output = new XMLWriter(bout, encoding, includeXMLDeclaration, pretty);
		if (namespaces == null)
			namespaces = new HashMap<>();
		if (!namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
			namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
		return new Async<>(
			this.output.start(rootNamespaceURI, rootLocalName, namespaces),
			ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> finalizeSerialization() {
		Async<SerializationException> sp = new Async<>();
		output.end().onDone(() -> bout.flush().onDone(sp, ioErrorConverter), sp, ioErrorConverter);
		return sp;
	}
	
	@Override
	protected IAsync<SerializationException> serializeBooleanValue(boolean value) {
		return new Async<>(output.addText(Boolean.toString(value)), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeNullValue() {
		return new Async<>(output.addAttribute("xsi:nil", "true"), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeCharacterValue(char value) {
		return new Async<>(output.addText(new String(new char[] { value })), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeNumericValue(Number value) {
		return new Async<>(output.addText(value.toString()), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeStringValue(CharSequence value) {
		return new Async<>(output.addText(value), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> startCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	) {
		return new Async<>(output.endOfAttributes(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> startCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	) {
		return new Async<>(output.openElement(null, "element", null), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> endCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	) {
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> endCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	) {
		return new Async<>(true);
	}
	
	protected static final Comparator<Attribute> attributesComparator = (o1, o2) -> {
		Class<?> c = o1.getType().getBase();
		if (c.isPrimitive()) return -1;
		if (Boolean.class.equals(c)) return -1;
		if (Number.class.isAssignableFrom(c)) return -1;
		if (CharSequence.class.isAssignableFrom(c)) return -1;
		if (Character.class.equals(c)) return -1;
		if (c.isEnum()) return -1;
		c = o2.getType().getBase();
		if (c.isPrimitive()) return 1;
		if (Boolean.class.equals(c)) return 1;
		if (Number.class.isAssignableFrom(c)) return 1;
		if (CharSequence.class.isAssignableFrom(c)) return 1;
		if (Character.class.equals(c)) return 1;
		if (c.isEnum()) return 1;
		return 0;
	};
	
	@Override
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		attributes.sort(attributesComparator);
		return attributes;
	}
	
	@Override
	@SuppressWarnings("squid:S1643")
	protected IAsync<SerializationException> startObjectValue(ObjectContext context, String path, List<SerializationRule> rules) {
		Object instance = context.getInstance();
		if (instance != null) {
			boolean customInstantiator = false;
			for (SerializationRule rule : rules)
				if (rule.canInstantiate(context.getOriginalType(), context)) {
					customInstantiator = true;
					break;
				}
			if (!customInstantiator && 
				(!(context.getParent() instanceof AttributeContext) ||
				 !((AttributeContext)context.getParent()).getAttribute().hasCustomInstantiation())
			) {
				Class<?> type = context.getOriginalType().getBase();
				if (!type.equals(instance.getClass())) {
					String attrName = "class";
					while (XMLDeserializer.hasAttribute(type, attrName)) attrName = "_" + attrName;
					return new Async<>(output.addAttribute(attrName, instance.getClass().getName()), ioErrorConverter);
				}
			}
		}
		return new Async<>(true);
	}
	
	@Override
	protected IAsync<SerializationException> endObjectValue(ObjectContext context, String path, List<SerializationRule> rules) {
		return new Async<>(true);
	}
	
	@Override
	protected IAsync<SerializationException> serializeNullAttribute(AttributeContext context, String path) {
		Class<?> c = context.getAttribute().getType().getBase();
		if (c.isPrimitive() ||
			Boolean.class.equals(c) ||
			Number.class.isAssignableFrom(c) ||
			CharSequence.class.isAssignableFrom(c) ||
			Character.class.equals(c) ||
			c.isEnum())
			return new Async<>(true);
		output.openElement(null, context.getAttribute().getName(), null);
		output.addAttribute("xsi:nil", "true");
		return new Async<>(output.closeElement(), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeBooleanAttribute(AttributeContext context, boolean value, String path) {
		return new Async<>(output.addAttribute(context.getAttribute().getName(), Boolean.toString(value)), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeNumericAttribute(AttributeContext context, Number value, String path) {
		return new Async<>(output.addAttribute(context.getAttribute().getName(), value.toString()), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeCharacterAttribute(AttributeContext context, char value, String path) {
		return new Async<>(output.addAttribute(context.getAttribute().getName(), new UnprotectedString(value)), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeStringAttribute(AttributeContext context, CharSequence value, String path) {
		return new Async<>(output.addAttribute(context.getAttribute().getName(), value), ioErrorConverter);
	}
	
	@Override
	protected IAsync<SerializationException> serializeObjectAttribute(
		AttributeContext context, Object value, String path, List<SerializationRule> rules
	) {
		output.openElement(null, context.getAttribute().getName(), null);
		IAsync<SerializationException> s =
			serializeObjectValue(context, value, context.getAttribute().getType(), path + '.' + context.getAttribute().getName(), rules);
		if (s.isDone()) {
			if (s.hasError()) return s;
			return new Async<>(output.closeElement(), ioErrorConverter);
		}
		Async<SerializationException> sp = new Async<>();
		s.thenStart(new SerializationTask(() -> output.closeElement().onDone(sp, ioErrorConverter)), sp);
		return sp;
	}
	
	@Override
	protected IAsync<SerializationException> serializeCollectionAttribute(
		CollectionContext context, String path, List<SerializationRule> rules
	) {
		Async<SerializationException> result = new Async<>();
		serializeCollectionAttributeElement(context, context.getIterator(), 0,
			path + '.' + ((AttributeContext)context.getParent()).getAttribute().getName(), rules, result);
		return result;
	}
	
	protected void serializeCollectionAttributeElement(
		CollectionContext context, Iterator<?> it, int elementIndex, String colPath,
		List<SerializationRule> rules, Async<SerializationException> result
	) {
		if (!it.hasNext()) {
			result.unblock();
			return;
		}
		Object element = it.next();
		String elementPath = colPath + '[' + elementIndex + ']';
		Attribute colAttr = ((AttributeContext)context.getParent()).getAttribute();
		output.openElement(null, colAttr.getName(), null);
		IAsync<SerializationException> value = serializeValue(context, element, context.getElementType(), elementPath, rules);
		if (value.isDone()) {
			if (value.hasError()) result.error(value.getError());
			else {
				output.closeElement();
				serializeCollectionAttributeElement(context, it, elementIndex + 1, colPath, rules, result);
			}
			return;
		}
		value.thenStart(new SerializationTask(() -> {
			output.closeElement();
			serializeCollectionAttributeElement(context, it, elementIndex + 1, colPath, rules, result);
		}), result);
	}

	@Override
	protected IAsync<SerializationException> serializeIOReadableValue(
		SerializationContext context, IO.Readable io, String path, List<SerializationRule> rules
	) {
		IAsync<IOException> encode = io.toReadConsumer(
			new Base64Encoding.EncoderConsumer<IOException>(
				UnprotectedStringIso8859.bytesConsumer(str -> output.addEscapedText(str)).convert(Bytes.Readable::toByteBuffer)
			).convert(RawByteBuffer::new)
		);
		if (encode.isDone()) {
			if (encode.hasError()) return new Async<>(encode, ioErrorConverter);
			return new Async<>(output.closeElement(), ioErrorConverter);
		}
		Async<SerializationException> sp = new Async<>();
		encode.thenStart(new SerializationTask(() -> output.closeElement()
			.onDone(sp, ioErrorConverter)),
			sp, ioErrorConverter);
		return sp;
	}

	@Override
	protected IAsync<SerializationException> serializeIOReadableAttribute(
		AttributeContext context, IO.Readable io, String path, List<SerializationRule> rules
	) {
		output.openElement(null, context.getAttribute().getName(), null);
		return serializeIOReadableValue(context, io, path, rules);
	}
	
	@Override
	protected IAsync<SerializationException> serializeAttribute(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		XMLCustomSerialization custom = context.getAttribute().getAnnotation(false, XMLCustomSerialization.class);
		if (custom == null)
			return super.serializeAttribute(context, path, rules);
		
		Object value;
		try { value = context.getAttribute().getValue(context.getParent().getInstance()); }
		catch (Exception e) {
			return new Async<>(
				new SerializationException("Unable to get value of attribute " + context.getAttribute().getOriginalName()
					+ " on " + context.getAttribute().getOriginalType().getClass().getName(), e));
		}
		
		if (value == null)
			return serializeNullAttribute(context, path);

		try {
			return custom.value().newInstance().serialize(value, this, output, rules);
		} catch (Exception e) {
			return new Async<>(new SerializationException("Error instantiating type", e));
		}
	}
	
}
