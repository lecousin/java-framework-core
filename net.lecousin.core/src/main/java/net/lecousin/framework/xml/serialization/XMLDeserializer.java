package net.lecousin.framework.xml.serialization;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.AbstractDeserializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.XMLUtil;

public class XMLDeserializer extends AbstractDeserializer<XMLStreamReaderAsync> {

	/** Constructor. */
	public XMLDeserializer() {
		this(null);
	}
	
	/** Constructor. */
	public XMLDeserializer(Charset encoding) {
		this.forceEncoding = encoding;
	}
	
	protected Charset forceEncoding;
	
	@Override
	protected AsyncWork<XMLStreamReaderAsync, Exception> initializeInput(IO.Readable input) {
		XMLStreamReaderAsync reader = new XMLStreamReaderAsync(input, forceEncoding, 8192);
		AsyncWork<XMLStreamReaderAsync, Exception> result = new AsyncWork<>();
		reader.startRootElement().listenInline(() -> { result.unblockSuccess(reader); }, result);
		return result;
	}
	
	@Override
	protected ISynchronizationPoint<Exception> finalizeInput(XMLStreamReaderAsync in) {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	protected AsyncWork<Boolean, Exception> deserializeBooleanValue(XMLStreamReaderAsync input, boolean nullable) {
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
	protected AsyncWork<? extends Number, Exception> deserializeNumericValue(Class<?> type, XMLStreamReaderAsync input, boolean nullable) {
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
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringValue(XMLStreamReaderAsync input) {
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true"))
			return new AsyncWork<>(null, null);
		return input.readInnerText();
	}
	
	private int objectValueAttributeIndex = 0;
	
	@Override
	protected AsyncWork<Object, Exception> startObjectValue(
		Object containerInstance, TypeDefinition type, XMLStreamReaderAsync input, List<SerializationRule> rules
	) {
		objectValueAttributeIndex = 0;
		XMLStreamReaderAsync.Attribute a = input.getAttributeWithNamespaceURI(XMLUtil.XSI_NAMESPACE_URI, "nil");
		if (a != null && a.value.equals("true"))
			return new AsyncWork<>(null, null);
		String attrName = "class";
		while (hasAttribute(type.getBase(), attrName)) attrName = "_" + attrName;
		a = input.getAttributeByLocalName(attrName);
		if (a != null) {
			String className = a.value.asString();
			try {
				Class<?> cl = Class.forName(className);
				return new AsyncWork<>(SerializationClass.instantiate(cl, rules), null);
			} catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
		}
		try {
			return new AsyncWork<>(SerializationClass.instantiate(type.getBase(), rules), null);
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
	protected AsyncWork<String, Exception> deserializeObjectAttributeName(XMLStreamReaderAsync input) {
		// first get attributes on element
		if (objectValueAttributeIndex < input.attributes.size())
			return new AsyncWork<>(input.attributes.get(objectValueAttributeIndex).localName.asString(), null);
		// then inner elements
		AsyncWork<Boolean, Exception> next = input.nextInnerElement();
		AsyncWork<String, Exception> result = new AsyncWork<>();
		next.listenInline((ok) -> {
			if (ok.booleanValue())
				result.unblockSuccess(input.text.asString());
			else
				result.unblockSuccess(null);
		}, result);
		return result;
	}
	
	@Override
	protected AsyncWork<Boolean, Exception> deserializeBooleanAttributeValue(Object containerInstance, Attribute a, XMLStreamReaderAsync input, boolean nullable) {
		if (objectValueAttributeIndex < input.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = input.attributes.get(objectValueAttributeIndex++);
			attr.value.toLowerCase();
			if (attr.value.equals("true") || attr.value.equals("yes") || attr.value.equals("1"))
				return new AsyncWork<>(Boolean.TRUE, null);
			else if (attr.value.equals("false") || attr.value.equals("no") || attr.value.equals("0"))
				return new AsyncWork<>(Boolean.FALSE, null);
			else
				return new AsyncWork<>(null, new Exception("Invalid boolean value: " + attr.value.asString()));
		}
		// inner element
		return deserializeBooleanValue(input, nullable);
	}
	
	@Override
	protected AsyncWork<? extends Number, Exception> deserializeNumericAttributeValue(Object containerInstance, Attribute a, XMLStreamReaderAsync input, boolean nullable) {
		if (objectValueAttributeIndex < input.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = input.attributes.get(objectValueAttributeIndex++);
			IntegerUnit.Unit unit = a.getAnnotation(false, IntegerUnit.Unit.class);
			if (unit != null) {
				try {
					return new AsyncWork<>(convertStringToInteger(a.getType().getBase(), attr.value.asString(), unit.value()), null);
				} catch (Exception e) {
					return new AsyncWork<>(null, e);
				}
			}
			AsyncWork<Number, Exception> result = new AsyncWork<>();
			try {
				BigDecimal n = new BigDecimal(attr.value.asString());
				convertBigDecimalValue(n, a.getType().getBase(), result);
			} catch (Exception e) {
				result.error(e);
			}
			return result;
		}
		// inner element
		return deserializeNumericValue(a.getType().getBase(), input, nullable);
	}
	
	@Override
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringAttributeValue(Object containerInstance, Attribute a, XMLStreamReaderAsync input) {
		if (objectValueAttributeIndex < input.attributes.size()) {
			// element attribute
			XMLStreamReaderAsync.Attribute attr = input.attributes.get(objectValueAttributeIndex++);
			return new AsyncWork<>(attr.value, null);
		}
		// inner element
		return deserializeStringValue(input);
	}
}
