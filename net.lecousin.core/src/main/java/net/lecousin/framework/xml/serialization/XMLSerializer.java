package net.lecousin.framework.xml.serialization;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedWritable;
import net.lecousin.framework.io.encoding.Base64;
import net.lecousin.framework.io.serialization.AbstractSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.xml.XMLUtil;
import net.lecousin.framework.xml.XMLWriter;

public class XMLSerializer extends AbstractSerializer {

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		this(rootNamespaceURI, rootLocalName, namespaces, null);
	}

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, null, includeXMLDeclaration);
	}

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096);
	}

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, boolean includeXMLDeclaration) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, 4096, includeXMLDeclaration);
	}

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize) {
		this(rootNamespaceURI, rootLocalName, namespaces, encoding, bufferSize, true);
	}

	public XMLSerializer(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces, Charset encoding, int bufferSize, boolean includeXMLDeclaration) {
		this.rootNamespaceURI = rootNamespaceURI;
		this.rootLocalName = rootLocalName;
		this.namespaces = namespaces;
		this.encoding = encoding;
		this.bufferSize = bufferSize;
		this.includeXMLDeclaration = includeXMLDeclaration;
	}
	
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
	
	@Override
	protected ISynchronizationPoint<IOException> initializeSerialization(IO.Writable output) {
		if (output instanceof IO.Writable.Buffered)
			bout = (IO.Writable.Buffered)output;
		else
			bout = new SimpleBufferedWritable(output, bufferSize);
		this.output = new XMLWriter(bout, encoding, includeXMLDeclaration);
		if (namespaces == null)
			namespaces = new HashMap<>();
		if (!namespaces.containsKey(XMLUtil.XSI_NAMESPACE_URI))
			namespaces.put(XMLUtil.XSI_NAMESPACE_URI, "xsi");
		return this.output.start(rootNamespaceURI, rootLocalName, namespaces);
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> finalizeSerialization() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		output.end().listenInlineSP(() -> {
			bout.flush().listenInlineSP(sp);
		}, sp);
		return sp;
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeBooleanValue(boolean value) {
		return output.addText(Boolean.toString(value));
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeNullValue() {
		return output.addAttribute("xsi:nil", "true");
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeCharacterValue(char value) {
		return output.addText(new String(new char[] { value }));
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeNumericValue(Number value) {
		return output.addText(value.toString());
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeStringValue(CharSequence value) {
		return output.addText(value);
	}
	
	@Override
	protected ISynchronizationPoint<IOException> startCollectionValue(CollectionContext context, String path, List<SerializationRule> rules) {
		return output.endOfAttributes();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> startCollectionValueElement(CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules) {
		return output.openElement(null, "element", null);
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> endCollectionValueElement(CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules) {
		return output.closeElement();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> endCollectionValue(CollectionContext context, String path, List<SerializationRule> rules) {
		return new SynchronizationPoint<>(true);
	}
	
	protected static final Comparator<Attribute> attributesComparator = new Comparator<Attribute>() {
		@Override
		public int compare(Attribute o1, Attribute o2) {
			Class<?> c = o1.getType().getBase();
			if (c.isPrimitive()) return -1;
			if (Boolean.class.equals(c)) return -1;
			if (Number.class.isAssignableFrom(c)) return -1;
			if (String.class.equals(c)) return -1;
			if (c.isEnum()) return -1;
			c = o2.getType().getBase();
			if (c.isPrimitive()) return 1;
			if (Boolean.class.equals(c)) return 1;
			if (Number.class.isAssignableFrom(c)) return 1;
			if (String.class.equals(c)) return 1;
			if (c.isEnum()) return 1;
			return 0;
		}
	};
	
	@Override
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		attributes.sort(attributesComparator);
		return attributes;
	}
	
	@Override
	protected ISynchronizationPoint<IOException> startObjectValue(ObjectContext context, String path, List<SerializationRule> rules) {
		Object instance = context.getInstance();
		if (instance != null) {
			if (!(context.getParent() instanceof AttributeContext) || !((AttributeContext)context.getParent()).getAttribute().hasCustomInstantiation()) {
				Class<?> type = context.getOriginalType().getBase();
				if (!type.equals(instance.getClass())) {
					String attrName = "class";
					while (XMLDeserializer.hasAttribute(type, attrName)) attrName = "_" + attrName;
					return output.addAttribute(attrName, instance.getClass().getName());
				}
			}
		}
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	protected ISynchronizationPoint<IOException> endObjectValue(ObjectContext context, String path, List<SerializationRule> rules) {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeNullAttribute(AttributeContext context, String path) {
		Class<?> c = context.getAttribute().getType().getBase();
		if (c.isPrimitive() ||
			Boolean.class.equals(c) ||
			Number.class.isAssignableFrom(c) ||
			String.class.equals(c) ||
			c.isEnum())
			return new SynchronizationPoint<>(true);
		output.openElement(null, context.getAttribute().getName(), null);
		output.addAttribute("xsi:nil", "true");
		return output.closeElement();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeBooleanAttribute(AttributeContext context, boolean value, String path) {
		return output.addAttribute(context.getAttribute().getName(), Boolean.toString(value));
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeNumericAttribute(AttributeContext context, Number value, String path) {
		return output.addAttribute(context.getAttribute().getName(), value.toString());
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeCharacterAttribute(AttributeContext context, char value, String path) {
		return output.addAttribute(context.getAttribute().getName(), new UnprotectedString(value));
	}
	
	@Override
	protected ISynchronizationPoint<IOException> serializeStringAttribute(AttributeContext context, CharSequence value, String path) {
		return output.addAttribute(context.getAttribute().getName(), value);
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> serializeObjectAttribute(AttributeContext context, Object value, String path, List<SerializationRule> rules) {
		output.openElement(null, context.getAttribute().getName(), null);
		ISynchronizationPoint<? extends Exception> s = serializeObjectValue(context, value, context.getAttribute().getType(), path + '.' + context.getAttribute().getName(), rules);
		if (s.isUnblocked()) {
			if (s.hasError()) return s;
			return output.closeElement();
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		s.listenAsyncSP(new SerializationTask(() -> {
			output.closeElement().listenInlineSP(sp);
		}), sp);
		return sp;
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> serializeCollectionAttribute(CollectionContext context, String path, List<SerializationRule> rules) {
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		serializeCollectionAttributeElement(context, context.getIterator(), 0, path + '.' + ((AttributeContext)context.getParent()).getAttribute().getName(), rules, result);
		return result;
	}
	
	protected void serializeCollectionAttributeElement(CollectionContext context, Iterator<?> it, int elementIndex, String colPath, List<SerializationRule> rules, SynchronizationPoint<Exception> result) {
		if (!it.hasNext()) {
			result.unblock();
			return;
		}
		Object element = it.next();
		String elementPath = colPath + '[' + elementIndex + ']';
		Attribute colAttr = ((AttributeContext)context.getParent()).getAttribute();
		output.openElement(null, colAttr.getName(), null);
		ISynchronizationPoint<? extends Exception> value = serializeValue(context, element, context.getElementType(), elementPath, rules);
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else {
				output.closeElement();
				serializeCollectionAttributeElement(context, it, elementIndex + 1, colPath, rules, result);
			}
			return;
		}
		value.listenAsyncSP(new SerializationTask(() -> {
			output.closeElement();
			serializeCollectionAttributeElement(context, it, elementIndex + 1, colPath, rules, result);
		}), result);
	}

	@Override
	protected ISynchronizationPoint<IOException> serializeIOReadableValue(SerializationContext context, IO.Readable io, String path, List<SerializationRule> rules) {
		return Base64.encodeAsync(io, new ICharacterStream.WriterAsync() {
			@Override
			public ISynchronizationPoint<IOException> writeAsync(char[] c, int offset, int length) {
				return output.addText(new UnprotectedString(c, offset, length, c.length));
			}
		});
	}

	@Override
	protected ISynchronizationPoint<IOException> serializeIOReadableAttribute(AttributeContext context, IO.Readable io, String path, List<SerializationRule> rules) {
		output.openElement(null, context.getAttribute().getName(), null);
		return serializeIOReadableValue(context, io, path, rules);
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> serializeAttribute(AttributeContext context, String path, List<SerializationRule> rules) {
		XMLCustomSerialization custom = context.getAttribute().getAnnotation(false, XMLCustomSerialization.class);
		if (custom == null)
			return super.serializeAttribute(context, path, rules);
		
		Object value;
		try { value = context.getAttribute().getValue(context.getParent().getInstance()); }
		catch (Exception e) {
			return new SynchronizationPoint<>(
				new Exception("Unable to get value of attribute " + context.getAttribute().getOriginalName()
					+ " on " + context.getAttribute().getOriginalType().getClass().getName(), e));
		}
		
		if (value == null)
			return serializeNullAttribute(context, path);

		try {
			return custom.value().newInstance().serialize(value, this, output, rules);
		} catch (Exception e) {
			return new SynchronizationPoint<>(e);
		}
	}
	
}
