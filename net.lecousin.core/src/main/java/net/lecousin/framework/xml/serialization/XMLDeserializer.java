package net.lecousin.framework.xml.serialization;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;
import net.lecousin.framework.io.encoding.Base64Decoder;
import net.lecousin.framework.io.serialization.AbstractDeserializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLUtil;

/** XML Deserialization. */
public class XMLDeserializer extends AbstractDeserializer {

	/** Constructor. */
	public XMLDeserializer(String expectedRootNamespaceURI, String expectedRootLocalName) {
		this(expectedRootNamespaceURI, expectedRootLocalName, null);
	}
	
	/** Constructor. */
	public XMLDeserializer(String expectedRootNamespaceURI, String expectedRootLocalName, Charset encoding) {
		this.expectedRootNamespaceURI = expectedRootNamespaceURI;
		this.expectedRootLocalName = expectedRootLocalName;
		this.forceEncoding = encoding;
	}
	
	/** Create a deserializer using a given XMLStreamEventsAsync, which must be positionned on the root element for the deserialization.
	 * Because we give directly a XMLStreamEventsAsync, the method to call must be deserializeValue instead of the usual deserialize.
	 */
	public XMLDeserializer(XMLStreamEventsAsync input, String expectedRootNamespaceURI, String expectedRootLocalName) {
		this.input = input;
		this.expectedRootNamespaceURI = expectedRootNamespaceURI;
		this.expectedRootLocalName = expectedRootLocalName;
	}
	
	protected String expectedRootNamespaceURI;
	protected String expectedRootLocalName;
	protected Charset forceEncoding;
	protected XMLStreamEventsAsync input;
	
	/** Deserialize from a file accessible from the classpath. */
	@SuppressWarnings("resource")
	public static <T> AsyncWork<T, Exception> deserializeResource(String resourcePath, Class<T> type, byte priority) {
		IO.Readable io = LCCore.getApplication().getResource(resourcePath, priority);
		if (io == null) return new AsyncWork<>(null, new FileNotFoundException("Resource not found: " + resourcePath));
		AsyncWork<T, Exception> result = deserialize(io, type);
		result.listenInline(() -> { io.closeAsync(); });
		return result;
	}
	
	/** Deserialize from a file. */
	@SuppressWarnings("resource")
	public static <T> AsyncWork<T, Exception> deserializeFile(File file, Class<T> type, byte priority) {
		IO.Readable io = new FileIO.ReadOnly(file, priority);
		AsyncWork<T, Exception> result = deserialize(io, type);
		result.listenInline(() -> { io.closeAsync(); });
		return result;
	}
	
	/** Deserialize from a IO.Readable. */
	@SuppressWarnings("unchecked")
	public static <T> AsyncWork<T, Exception> deserialize(IO.Readable input, Class<T> type) {
		XMLDeserializer deserializer = new XMLDeserializer(null, type.getSimpleName());
		AsyncWork<Object, Exception> res = deserializer.deserialize(new TypeDefinition(type), input, new ArrayList<>(0));
		AsyncWork<T, Exception> result = new AsyncWork<>();
		res.listenInline((obj) -> {
			result.unblockSuccess((T)obj);
		}, result);
		return result;
	}
	
	protected ISynchronizationPoint<Exception> createAndStartReader(IO.Readable input) {
		XMLStreamReaderAsync reader = new XMLStreamReaderAsync(input, forceEncoding, 8192, 4);
		this.input = reader;
		reader.setMaximumTextSize(maxTextSize);
		reader.setMaximumCDataSize(maxTextSize);
		return reader.startRootElement();
	}
	
	@Override
	public void setMaximumTextSize(int max) {
		super.setMaximumTextSize(max);
		this.input.setMaximumTextSize(maxTextSize);
		this.input.setMaximumCDataSize(maxTextSize);
	}
	
	@Override
	protected ISynchronizationPoint<Exception> initializeDeserialization(IO.Readable input) {
		ISynchronizationPoint<Exception> start = createAndStartReader(input);
		if (start.isUnblocked()) {
			if (start.hasError()) return start;
			if (this.expectedRootLocalName != null &&
				!this.input.event.localName.equals(this.expectedRootLocalName))
				return new SynchronizationPoint<>(new Exception("Expected root XML element is " + this.expectedRootLocalName
					+ ", found is " + this.input.event.localName.asString()));
			if (this.expectedRootNamespaceURI != null &&
				!this.input.getNamespaceURI(this.input.event.namespacePrefix).equals(this.expectedRootNamespaceURI))
				return new SynchronizationPoint<>(new Exception("Expected root XML element namespace is "
					+ this.expectedRootNamespaceURI	+ ", found is "
					+ this.input.getNamespaceURI(this.input.event.namespacePrefix)));
			return start;
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		start.listenInline(() -> {
			if (this.expectedRootLocalName != null &&
				!this.input.event.localName.equals(this.expectedRootLocalName))
				sp.error(new Exception("Expected root XML element is " + this.expectedRootLocalName
					+ ", found is " + this.input.event.localName.asString()));
			else if (this.expectedRootNamespaceURI != null &&
					!this.input.getNamespaceURI(this.input.event.namespacePrefix).equals(this.expectedRootNamespaceURI))
				sp.error(new Exception("Expected root XML element namespace is " + this.expectedRootNamespaceURI + ", found is "
					+ this.input.getNamespaceURI(this.input.event.namespacePrefix)));
			else
				sp.unblock();
		}, sp);
		return sp;
	}
	
	@Override
	protected ISynchronizationPoint<Exception> finalizeDeserialization() {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	protected AsyncWork<Boolean, Exception> deserializeBooleanValue(boolean nullable) {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true")) {
			if (nullable)
				return new AsyncWork<>(null, null);
			return new AsyncWork<>(null, new Exception("null value found but boolean expected"));
		}
		AsyncWork<UnprotectedStringBuffer, Exception> read = input.readInnerText();
		AsyncWork<Boolean, Exception> result = new AsyncWork<>();
		read.listenInline((text) -> {
			text.toLowerCase();
			if (text.equals("true") || text.equals("yes") || text.equals("1"))
				result.unblockSuccess(Boolean.TRUE);
			else if (text.equals("false") || text.equals("no") || text.equals("0"))
				result.unblockSuccess(Boolean.FALSE);
			else
				result.error(new Exception("Invalid boolean value: " + text.asString()));
		}, result);
		return result;
	}
	
	@Override
	protected AsyncWork<? extends Number, Exception> deserializeNumericValue(
		Class<?> type, boolean nullable, Class<? extends IntegerUnit> targetUnit
	) {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true")) {
			if (nullable)
				return new AsyncWork<>(null, null);
			return new AsyncWork<>(null, new Exception("null value found but number expected"));
		}
		AsyncWork<UnprotectedStringBuffer, Exception> read = input.readInnerText();
		AsyncWork<Number, Exception> result = new AsyncWork<>();
		read.listenInline((text) -> {
			try {
				if (targetUnit != null)
					result.unblockSuccess(convertStringToInteger(type, text.asString(), targetUnit));
				else
					convertBigDecimalValue(new BigDecimal(text.asString()), type, result);
			} catch (Exception e) {
				result.error(e);
			}
		}, result);
		return result;
	}
	
	@Override
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringValue() {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true"))
			return new AsyncWork<>(null, null);
		return input.readInnerText();
	}
	
	private static class CollectionValueContext {
		public ElementContext parent;
	}
	
	private LinkedList<CollectionValueContext> colValueContext = new LinkedList<>();
	
	@Override
	protected AsyncWork<Boolean, Exception> startCollectionValue() {
		CollectionValueContext ctx = new CollectionValueContext();
		ctx.parent = input.event.context.getFirst();
		colValueContext.addFirst(ctx);
		// there is no specific start for a collection value
		return new AsyncWork<>(Boolean.TRUE, null);
	}
	
	@Override
	protected AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules
	) {
		CollectionValueContext ctx = colValueContext.getFirst();
		AsyncWork<Boolean, Exception> read = input.nextInnerElement(ctx.parent, "element");
		if (read.isUnblocked()) {
			if (read.hasError()) return new AsyncWork<>(null, read.getError());
			if (!read.getResult().booleanValue()) {
				colValueContext.removeFirst();
				return new AsyncWork<>(new Pair<>(null, Boolean.FALSE), null);
			}
			AsyncWork<Object, Exception> element =
				deserializeValue(context, context.getElementType(), colPath + '[' + elementIndex + ']', rules);
			if (element.isUnblocked()) {
				if (element.hasError()) return new AsyncWork<>(null, element.getError());
				return new AsyncWork<>(new Pair<>(element.getResult(), Boolean.TRUE), null);
			}
			AsyncWork<Pair<Object, Boolean>, Exception> result = new AsyncWork<>();
			element.listenInline(() -> {
				result.unblockSuccess(new Pair<>(element.getResult(), Boolean.TRUE));
			}, result);
			return result;
		}
		AsyncWork<Pair<Object, Boolean>, Exception> result = new AsyncWork<>();
		read.listenAsync(new DeserializationTask(() -> {
			if (!read.getResult().booleanValue()) {
				colValueContext.removeFirst();
				result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
				return;
			}
			AsyncWork<Object, Exception> element =
				deserializeValue(context, context.getElementType(), colPath + '[' + elementIndex + ']', rules);
			if (element.isUnblocked()) {
				if (element.hasError()) result.error(element.getError());
				else result.unblockSuccess(new Pair<>(element.getResult(), Boolean.TRUE));
				return;
			}
			element.listenInline(() -> {
				result.unblockSuccess(new Pair<>(element.getResult(), Boolean.TRUE));
			}, result);
		}), result);
		return result;
	}

	private static class XMLObjectContext {
		private ElementContext element;
		private int attributeIndex = 0;
		private List<XMLStreamReaderAsync.Attribute> attributes;
		private boolean endOfAttributes = false;
		private boolean onNextAttribute = false;
		private List<String> attributesDone = new LinkedList<>();
	}
	
	private LinkedList<XMLObjectContext> objects = new LinkedList<>();
	
	@Override
	protected AsyncWork<Object, Exception> startObjectValue(
		SerializationContext context, TypeDefinition type, List<SerializationRule> rules
	) {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true")) {
			ISynchronizationPoint<Exception> close = input.closeElement();
			if (close.isUnblocked())
				return new AsyncWork<>(null, close.getError());
			AsyncWork<Object, Exception> res = new AsyncWork<>();
			close.listenInline(() -> { res.unblockSuccess(null); }, res);
			return res;
		}
		XMLObjectContext ctx = new XMLObjectContext();
		ctx.element = input.event.context.getFirst();
		ctx.attributes = input.event.attributes;
		ctx.endOfAttributes = input.event.isClosed;
		objects.addFirst(ctx);
		String attrName = "class";
		while (hasAttribute(type.getBase(), attrName)) attrName = "_" + attrName;
		a = input.removeAttributeByLocalName(attrName);
		if (a != null) {
			String className = a.value.asString();
			try {
				Class<?> cl = Class.forName(className);
				return new AsyncWork<>(SerializationClass.instantiate(new TypeDefinition(cl), context, rules, true), null);
			} catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
		}
		try {
			return new AsyncWork<>(SerializationClass.instantiate(type, context, rules, false), null);
		} catch (Exception e) {
			return new AsyncWork<>(null, e);
		}
	}
	
	/** Return true if the given class contains the requested attribute, either as a field or with a getter or setter. */
	public static boolean hasAttribute(Class<?> type, String name) {
		if (type.equals(Object.class)) return false;
		for (Field f : type.getDeclaredFields())
			if (f.getName().equals(name))
				return true;
		Method m;
		m = ClassUtil.getGetter(type, name);
		if (m != null && !m.getDeclaringClass().equals(Object.class)) return true;
		m = ClassUtil.getSetter(type, name);
		if (m != null && !m.getDeclaringClass().equals(Object.class)) return true;
		if (type.getSuperclass() != null)
			return hasAttribute(type.getSuperclass(), name);
		return false;
	}
	
	@Override
	protected AsyncWork<String, Exception> deserializeObjectAttributeName(ObjectContext context) {
		XMLObjectContext ctx = objects.getFirst();
		// first get attributes on element
		if (ctx.attributeIndex < ctx.attributes.size())
			return new AsyncWork<>(ctx.attributes.get(ctx.attributeIndex).localName.asString(), null);
		// then inner elements
		if (ctx.endOfAttributes) {
			try { endOfAttributes(ctx, context); }
			catch (Exception e) { return new AsyncWork<>(null, e); }
			objects.removeFirst();
			return new AsyncWork<>(null, null);
		}
		AsyncWork<Boolean, Exception> next;
		if (ctx.onNextAttribute) {
			next = new AsyncWork<>(Boolean.TRUE, null);
			ctx.onNextAttribute = false;
		} else
			next = input.nextInnerElement(ctx.element);
		AsyncWork<String, Exception> result = new AsyncWork<>();
		next.listenInline(() -> {
			if (next.hasError()) {
				if (next.getError() instanceof EOFException) {
					objects.removeFirst();
					result.unblockSuccess(null);
					return;
				}
				result.error(next.getError());
				return;
			}
			if (next.getResult().booleanValue()) {
				String name = input.event.text.asString();
				ctx.attributesDone.add(name);
				result.unblockSuccess(name);
			} else {
				try { endOfAttributes(ctx, context); }
				catch (Exception e) {
					result.error(e);
					return;
				}
				objects.removeFirst();
				result.unblockSuccess(null);
			}
		});
		return result;
	}
	
	private static void endOfAttributes(XMLObjectContext ctx, ObjectContext context) throws Exception {
		// In XML, if an attribute is not present, it means it is null
		for (Attribute a : context.getSerializationClass().getAttributes()) {
			String name = a.getName();
			if (!a.canSet()) continue;
			if (a.ignore()) continue;
			if (ctx.attributesDone.contains(name)) continue;
			boolean found = false;
			for (XMLStreamEvents.Attribute xmlAttr : ctx.attributes)
				if (xmlAttr.localName.equals(name)) {
					found = true;
					break;
				}
			if (found) continue;
			// not found => set as null
			// except if it is a primitive
			if (a.getType().getBase().isPrimitive()) continue;
			// except if it is a collection
			if (Collection.class.isAssignableFrom(a.getType().getBase())) continue;
			a.setValue(context.getInstance(), null);
		}
	}
	
	@Override
	protected AsyncWork<Boolean, Exception> deserializeBooleanAttributeValue(AttributeContext context, boolean nullable) {
		XMLObjectContext ctx = objects.getFirst();
		if (ctx.attributeIndex < ctx.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = ctx.attributes.get(ctx.attributeIndex++);
			attr.value.toLowerCase();
			if (attr.value.equals("true") || attr.value.equals("yes") || attr.value.equals("1"))
				return new AsyncWork<>(Boolean.TRUE, null);
			else if (attr.value.equals("false") || attr.value.equals("no") || attr.value.equals("0"))
				return new AsyncWork<>(Boolean.FALSE, null);
			else
				return new AsyncWork<>(null, new Exception("Invalid boolean value: " + attr.value.asString()));
		}
		// inner element
		return deserializeBooleanValue(nullable);
	}
	
	@Override
	protected AsyncWork<? extends Number, Exception> deserializeNumericAttributeValue(AttributeContext context, boolean nullable) {
		XMLObjectContext ctx = objects.getFirst();
		IntegerUnit.Unit unit = context.getAttribute().getAnnotation(false, IntegerUnit.Unit.class);
		if (ctx.attributeIndex < ctx.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = ctx.attributes.get(ctx.attributeIndex++);
			if (unit != null) {
				try {
					return new AsyncWork<>(convertStringToInteger(context.getAttribute().getType().getBase(),
						attr.value.asString(), unit.value()), null);
				} catch (Exception e) {
					return new AsyncWork<>(null, e);
				}
			}
			AsyncWork<Number, Exception> result = new AsyncWork<>();
			try {
				BigDecimal n = new BigDecimal(attr.value.asString());
				convertBigDecimalValue(n, context.getAttribute().getType().getBase(), result);
			} catch (Exception e) {
				result.error(e);
			}
			return result;
		}
		// inner element
		return deserializeNumericValue(context.getAttribute().getType().getBase(), nullable, unit != null ? unit.value() : null);
	}
	
	@Override
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringAttributeValue(AttributeContext context) {
		XMLObjectContext ctx = objects.getFirst();
		if (ctx.attributeIndex < ctx.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = ctx.attributes.get(ctx.attributeIndex++);
			return new AsyncWork<>(attr.value, null);
		}
		// inner element
		return deserializeStringValue();
	}
	
	@Override
	protected AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionAttributeValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules
	) {
		XMLObjectContext ctx = objects.getFirst();
		Attribute colAttr = ((AttributeContext)context.getParent()).getAttribute();
		AsyncWork<Pair<Object, Boolean>, Exception> result = new AsyncWork<>();
		if (elementIndex > 0) {
			// we need to go to the next element
			AsyncWork<Boolean, Exception> next = input.nextInnerElement(ctx.element);
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return result;
				}
				if (!next.getResult().booleanValue()) {
					ctx.endOfAttributes = true;
					result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
					return result;
				}
				if (!input.event.text.equals(colAttr.getName())) {
					ctx.onNextAttribute = true;
					result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
					return result;
				}
			} else {
				next.listenAsync(new DeserializationTask(() -> {
					if (!next.getResult().booleanValue()) {
						ctx.endOfAttributes = true;
						result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
						return;
					}
					if (!input.event.text.equals(colAttr.getName())) {
						ctx.onNextAttribute = true;
						result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
						return;
					}
					readColElement(context, colPath + '[' + elementIndex + ']', rules, result);
				}), result);
				return result;
			}
		}
		
		readColElement(context, colPath + '[' + elementIndex + ']', rules, result);
		return result;
	}
	
	private void readColElement(
		CollectionContext context, String elementPath, List<SerializationRule> rules, AsyncWork<Pair<Object, Boolean>, Exception> result
	) {
		Attribute a = null;
		SerializationContext c = context.getParent();
		while (c != null) {
			if (c instanceof AttributeContext) {
				a = ((AttributeContext)c).getAttribute();
				break;
			}
			c = c.getParent();
		}
		XMLCustomSerialization custom = a != null ? a.getAnnotation(false, XMLCustomSerialization.class) : null;
		AsyncWork<?, Exception> value = null;
		if (custom != null) {
			try {
				XMLCustomSerializer s = custom.value().newInstance();
				if (s.type().equals(context.getElementType()))
					value = s.deserialize(this, input, rules);
			} catch (Exception e) {
				result.error(e);
				return;
			}
		}
		if (value == null)
			value = deserializeValue(context, context.getElementType(), elementPath, rules);
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else result.unblockSuccess(new Pair<>(value.getResult(), Boolean.TRUE));
		} else
			value.listenInline((obj) -> { result.unblockSuccess(new Pair<>(obj, Boolean.TRUE)); }, result);
	}

	@SuppressWarnings("resource")
	@Override
	protected AsyncWork<IO.Readable, Exception> deserializeIOReadableValue(SerializationContext context, List<SerializationRule> rules) {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true"))
			return new AsyncWork<>(null, null);
		ISynchronizationPoint<Exception> next = input.next();
		AsyncWork<IO.Readable, Exception> result = new AsyncWork<>();
		next.listenAsync(new DeserializationTask(() -> {
			if (Type.TEXT.equals(input.event.type)) {
				// it may be a reference
				String ref = input.event.text.asString();
				for (StreamReferenceHandler h : streamReferenceHandlers)
					if (h.isReference(ref)) {
						h.getStreamFromReference(ref).listenInline(result);
						return;
					}
			}
			// not a reference, default to base 64 encoded string
			IOInMemoryOrFile io = new IOInMemoryOrFile(128 * 1024, priority, "base 64 encoded from XML");
			Base64Decoder decoder = new Base64Decoder(io);
			readBase64(decoder, io, result);
		}), result);
		return result;
	}
	
	private void readNextBase64(Base64Decoder decoder, IOInMemoryOrFile io, AsyncWork<IO.Readable, Exception> result) {
		ISynchronizationPoint<Exception> next = input.next();
		if (next.isUnblocked()) {
			if (next.hasError()) result.error(next.getError());
			else readBase64(decoder, io, result);
			return;
		}
		next.listenAsync(new DeserializationTask(() -> { readBase64(decoder, io, result); }), result);
	}
	
	private void readBase64(Base64Decoder decoder, IOInMemoryOrFile io, AsyncWork<IO.Readable, Exception> result) {
		if (Type.TEXT.equals(input.event.type)) {
			input.event.text.trim();
			if (input.event.text.length() == 0) {
				readNextBase64(decoder, io, result);
				return;
			}
			CharBuffer[] buffers = input.event.text.asCharBuffers();
			decodeBase64(decoder, io, result, buffers, 0);
			return;
		}
		if (Type.START_ELEMENT.equals(input.event.type)) {
			input.closeElement().listenAsync(new DeserializationTask(() -> {
				readNextBase64(decoder, io, result);
			}), result);
			return;
		}
		if (Type.END_ELEMENT.equals(input.event.type)) {
			decoder.flush().listenInlineSP(() -> {
				io.seekAsync(SeekType.FROM_BEGINNING, 0).listenInlineSP(() -> {
					result.unblockSuccess(io);
				}, result);
			}, result);
			return;
		}
		readNextBase64(decoder, io, result);
	}
	
	private void decodeBase64(
		Base64Decoder decoder, IOInMemoryOrFile io, AsyncWork<IO.Readable, Exception> result, CharBuffer[] buffers, int index
	) {
		ISynchronizationPoint<IOException> decode = decoder.decode(buffers[index]);
		decode.listenAsync(new DeserializationTask(() -> {
			if (decode.hasError())
				result.error(decode.getError());
			else if (index == buffers.length - 1)
				readNextBase64(decoder, io, result);
			else
				decodeBase64(decoder, io, result, buffers, index + 1);
		}), true);
	}

	@Override
	protected AsyncWork<IO.Readable, Exception> deserializeIOReadableAttributeValue(AttributeContext context, List<SerializationRule> rules) {
		return deserializeIOReadableValue(context, rules);
	}
	
	@Override
	protected AsyncWork<?, Exception> deserializeObjectAttributeValue(AttributeContext context, String path, List<SerializationRule> rules) {
		XMLCustomSerialization custom = context.getAttribute().getAnnotation(false, XMLCustomSerialization.class);
		if (custom != null) {
			try {
				XMLCustomSerializer s = custom.value().newInstance();
				if (s.type().equals(context.getAttribute().getType()))
					return s.deserialize(this, input, rules);
			} catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
		}
		return super.deserializeObjectAttributeValue(context, path, rules);
	}
}
