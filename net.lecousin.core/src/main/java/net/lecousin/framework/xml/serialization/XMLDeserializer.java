package net.lecousin.framework.xml.serialization;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;
import net.lecousin.framework.io.encoding.Base64Decoder;
import net.lecousin.framework.io.serialization.AbstractDeserializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLUtil;

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
	
	@SuppressWarnings({ "unchecked", "resource" })
	public static <T> AsyncWork<T, Exception> deserializeResource(String resourcePath, Class<T> type, byte priority) {
		IO.Readable io = LCCore.getApplication().getResource(resourcePath, priority);
		XMLDeserializer deserializer = new XMLDeserializer(null, type.getSimpleName());
		AsyncWork<Object, Exception> res = deserializer.deserialize(new TypeDefinition(type), io, new ArrayList<>(0));
		AsyncWork<T, Exception> result = new AsyncWork<>();
		res.listenInline(() -> {
			io.closeAsync();
			if (res.hasError()) result.error(res.getError());
			else if (res.isCancelled()) result.cancel(res.getCancelEvent());
			else result.unblockSuccess((T)res.getResult());
		});
		return result;
	}
	
	@Override
	protected ISynchronizationPoint<Exception> initializeDeserialization(IO.Readable input) {
		XMLStreamReaderAsync reader = new XMLStreamReaderAsync(input, forceEncoding, 8192);
		this.input = reader;
		reader.setMaximumTextSize(16384);
		reader.setMaximumCDataSize(16384);
		ISynchronizationPoint<Exception> start = reader.startRootElement();
		if (start.isUnblocked()) {
			if (start.hasError()) return start;
			if (this.expectedRootLocalName != null && !this.input.event.localName.equals(this.expectedRootLocalName))
				return new SynchronizationPoint<>(new Exception("Expected root XML element is " + this.expectedRootLocalName + ", found is " + this.input.event.localName.asString()));
			if (this.expectedRootNamespaceURI != null && !this.expectedRootNamespaceURI.equals(this.input.getNamespaceURI(this.input.event.namespacePrefix)))
				return new SynchronizationPoint<>(new Exception("Expected root XML element namespace is " + this.expectedRootNamespaceURI + ", found is " + this.input.getNamespaceURI(this.input.event.namespacePrefix)));
			return start;
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		start.listenInline(() -> {
			if (this.expectedRootLocalName != null && !this.input.event.localName.equals(this.expectedRootLocalName))
				sp.error(new Exception("Expected root XML element is " + this.expectedRootLocalName + ", found is " + this.input.event.localName.asString()));
			else if (this.expectedRootNamespaceURI != null && !this.expectedRootNamespaceURI.equals(this.input.getNamespaceURI(this.input.event.namespacePrefix)))
				sp.error(new Exception("Expected root XML element namespace is " + this.expectedRootNamespaceURI + ", found is " + this.input.getNamespaceURI(this.input.event.namespacePrefix)));
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
	protected AsyncWork<? extends Number, Exception> deserializeNumericValue(Class<?> type, boolean nullable) {
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
				BigDecimal n = new BigDecimal(text.asString());
				convertBigDecimalValue(n, type, result);
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
	
	@Override
	protected AsyncWork<Boolean, Exception> startCollectionValue() {
		// there is no specific start for a collection value
		return new AsyncWork<>(Boolean.TRUE, null);
	}
	
	@Override
	protected AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionValueElement(
		CollectionContext context, int elementIndex, List<SerializationRule> rules
	) {
		AsyncWork<Boolean, Exception> read = input.nextInnerElement("element");
		if (read.isUnblocked()) {
			if (read.hasError()) return new AsyncWork<>(null, read.getError());
			if (!read.getResult().booleanValue()) return new AsyncWork<>(new Pair<>(null, Boolean.FALSE), null);
			Object element;
			try { element = SerializationClass.instantiate(context.getElementType(), context, rules); }
			catch (Exception e) { return new AsyncWork<>(null, e); }
			return new AsyncWork<>(new Pair<>(element, Boolean.TRUE), null);
		}
		AsyncWork<Pair<Object, Boolean>, Exception> result = new AsyncWork<>();
		read.listenAsync(new DeserializationTask(() -> {
			if (!read.getResult().booleanValue()) {
				result.unblockSuccess(new Pair<>(null, Boolean.FALSE));
				return;
			}
			try {
				Object element = SerializationClass.instantiate(context.getElementType(), context, rules);
				result.unblockSuccess(new Pair<>(element, Boolean.TRUE));
			} catch (Exception e) { result.error(e); }
		}), result);
		return result;
	}

	private static class ObjectContext {
		private int attributeIndex = 0;
		private List<XMLStreamReaderAsync.Attribute> attributes;
		private boolean endOfAttributes = false;
		private boolean onNextAttribute = false;
	}
	
	private LinkedList<ObjectContext> objects = new LinkedList<>();
	
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
		ObjectContext ctx = new ObjectContext();
		ctx.attributes = input.event.attributes;
		ctx.endOfAttributes = input.event.isClosed;
		objects.addFirst(ctx);
		String attrName = "class";
		while (hasAttribute(type.getBase(), attrName)) attrName = "_" + attrName;
		a = input.getAttributeByLocalName(attrName);
		if (a != null) {
			String className = a.value.asString();
			try {
				Class<?> cl = Class.forName(className);
				return new AsyncWork<>(SerializationClass.instantiate(new TypeDefinition(cl), context, rules), null);
			} catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
		}
		try {
			return new AsyncWork<>(SerializationClass.instantiate(type, context, rules), null);
		} catch (Exception e) {
			return new AsyncWork<>(null, e);
		}
	}
	
	private boolean hasAttribute(Class<?> type, String name) {
		for (Field f : type.getDeclaredFields())
			if (f.getName().equals(name))
				return true;
		if (ClassUtil.getGetter(type, name) != null) return true;
		if (ClassUtil.getSetter(type, name) != null) return true;
		if (type.getSuperclass() != null)
			return hasAttribute(type.getSuperclass(), name);
		return false;
	}
	
	@Override
	protected AsyncWork<String, Exception> deserializeObjectAttributeName() {
		ObjectContext ctx = objects.getFirst();
		// first get attributes on element
		if (ctx.attributeIndex < ctx.attributes.size())
			return new AsyncWork<>(ctx.attributes.get(ctx.attributeIndex).localName.asString(), null);
		// then inner elements
		if (ctx.endOfAttributes) {
			objects.removeFirst();
			return new AsyncWork<>(null, null);
		}
		AsyncWork<Boolean, Exception> next;
		if (ctx.onNextAttribute) {
			next = new AsyncWork<>(Boolean.TRUE, null);
			ctx.onNextAttribute = false;
		} else
			next = input.nextInnerElement();
		AsyncWork<String, Exception> result = new AsyncWork<>();
		next.listenInline((ok) -> {
			if (ok.booleanValue())
				result.unblockSuccess(input.event.text.asString());
			else
				result.unblockSuccess(null);
		}, result);
		return result;
	}
	
	@Override
	protected AsyncWork<Boolean, Exception> deserializeBooleanAttributeValue(AttributeContext context, boolean nullable) {
		ObjectContext ctx = objects.getFirst();
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
		ObjectContext ctx = objects.getFirst();
		if (ctx.attributeIndex < ctx.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = ctx.attributes.get(ctx.attributeIndex++);
			IntegerUnit.Unit unit = context.getAttribute().getAnnotation(false, IntegerUnit.Unit.class);
			if (unit != null) {
				try {
					return new AsyncWork<>(convertStringToInteger(context.getAttribute().getType().getBase(), attr.value.asString(), unit.value()), null);
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
		return deserializeNumericValue(context.getAttribute().getType().getBase(), nullable);
	}
	
	@Override
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringAttributeValue(AttributeContext context) {
		ObjectContext ctx = objects.getFirst();
		if (ctx.attributeIndex < ctx.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = ctx.attributes.get(ctx.attributeIndex++);
			return new AsyncWork<>(attr.value, null);
		}
		// inner element
		return deserializeStringValue();
	}
	
	@Override
	protected AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionAttributeValueElement(CollectionContext context, int elementIndex, List<SerializationRule> rules) {
		ObjectContext ctx = objects.getFirst();
		Attribute colAttr = ((AttributeContext)context.getParent()).getAttribute();
		AsyncWork<Pair<Object, Boolean>, Exception> result = new AsyncWork<>();
		if (elementIndex > 0) {
			// we need to go to the next element
			AsyncWork<Boolean, Exception> next = input.nextInnerElement();
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
					readColElement(context, rules, result);
				}), result);
				return result;
			}
		}
		
		readColElement(context, rules, result);
		return result;
	}
	
	private void readColElement(CollectionContext context, List<SerializationRule> rules, AsyncWork<Pair<Object, Boolean>, Exception> result) {
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
			value = deserializeValue(context, context.getElementType(), rules);
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else result.unblockSuccess(new Pair<>(value.getResult(), Boolean.TRUE));
		} else
			value.listenInline((obj) -> { result.unblockSuccess(new Pair<>(obj, Boolean.TRUE)); }, result);
	}

	@SuppressWarnings("resource")
	@Override
	protected AsyncWork<IO.Readable, Exception> deserializeIOReadableValue(SerializationContext context, List<SerializationRule> rules) {
		IOInMemoryOrFile io = new IOInMemoryOrFile(128 * 1024, priority, "base 64 encoded from XML");
		Base64Decoder decoder = new Base64Decoder(io);
		AsyncWork<IO.Readable, Exception> result = new AsyncWork<>();
		readNextBase64(decoder, io, result);
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
			decoder.flush().listenInlineSP(() -> { result.unblockSuccess(io); }, result);
			return;
		}
		readNextBase64(decoder, io, result);
	}
	
	private void decodeBase64(Base64Decoder decoder, IOInMemoryOrFile io, AsyncWork<IO.Readable, Exception> result, CharBuffer[] buffers, int index) {
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
	protected AsyncWork<?, Exception> deserializeObjectAttributeValue(AttributeContext context, List<SerializationRule> rules) {
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
		return super.deserializeObjectAttributeValue(context, rules);
	}
}
