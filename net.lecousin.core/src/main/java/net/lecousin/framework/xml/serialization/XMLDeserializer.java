package net.lecousin.framework.xml.serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.serialization.AbstractDeserializer;
import net.lecousin.framework.io.serialization.SerializationUtil;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamCursor;

/** XML deserialization. */
public class XMLDeserializer extends AbstractDeserializer<IO.Readable> {

	@Override
	protected Readable adaptInput(Readable input) {
		return input;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(Class<T> type, ParameterizedType ptype, Readable input, List<SerializationRule> rules) throws Exception {
		XMLStreamCursor reader = new XMLStreamCursor(input);
		reader.startRootElement();
		return (T)deserializeObject(type, null, null, reader, rules);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object deserializeObject(
		Class<?> type, Attribute containerAttribute, Object containerInstance, XMLStreamCursor reader, List<SerializationRule> rules
	) throws Exception {
		if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			if (reader.isClosed) return null;
			return Boolean.valueOf(reader.readInnerText().trim().asString());
		}
		if (byte.class.equals(type))
			return Byte.valueOf(reader.readInnerText().trim().asString());
		if (short.class.equals(type))
			return Short.valueOf(reader.readInnerText().trim().asString());
		if (int.class.equals(type))
			return Integer.valueOf(reader.readInnerText().trim().asString());
		if (long.class.equals(type))
			return Long.valueOf(reader.readInnerText().trim().asString());
		if (float.class.equals(type))
			return Float.valueOf(reader.readInnerText().trim().asString());
		if (double.class.equals(type))
			return Double.valueOf(reader.readInnerText().trim().asString());
		
		if (Number.class.isAssignableFrom(type)) {
			try {
				Constructor<?> ctor = type.getConstructor(String.class);
				return ctor.newInstance(reader.readInnerText().trim());
			} catch (NoSuchMethodException e) {
				throw new Exception("Unsupported Number type " + type.getName());
			}
		}
		if (char.class.equals(type) || Character.class.equals(type)) {
			UnprotectedStringBuffer s = reader.readInnerText();
			if (s.length() == 0) return null;
			return Character.valueOf(s.charAt(0));
		}
		if (String.class.isAssignableFrom(type))
			return reader.readInnerText();
		if (type.isEnum()) {
			UnprotectedStringBuffer name = reader.readInnerText();
			if (name.length() == 0) return null;
			return Enum.valueOf((Class<? extends Enum>)type, name.asString());
		}

		Object obj;
		if (containerInstance == null)
			obj = type.newInstance();
		else {
			obj = containerAttribute.instantiate(containerInstance);
			type = obj.getClass();
		}
		
		ArrayList<Attribute> attributes = SerializationUtil.getAttributes(type);
		for (SerializationRule rule : rules)
			rule.apply(attributes);
		for (SerializationRule rule : SerializationUtil.processAnnotations(attributes, true, false))
			rule.apply(attributes);
		SerializationUtil.removeIgnoredAttributes(attributes);
		
		deserializeAttributes(reader, obj, attributes);
		if (reader.isClosed)
			return obj;
		String elementName = reader.text.toString();
		Map<String,Triple<Collection<Object>,Class<?>,Attribute>> collections = new HashMap<>();
		while (reader.nextInnerElement(elementName)) {
			String name = reader.text.toString();
			
			Triple<Collection<Object>,Class<?>,Attribute> col = collections.get(name);
			if (col == null) {
				Attribute a = SerializationUtil.getAttributeByName(attributes, name);
				if (a == null)
					throw new Exception("Unknown attribute '" + name + "' in class " + type.getName());
				if (!a.canSet())
					throw new Exception("Attribute '" + name + "' in class " + type.getName() + " cannot be set");
				
				if (Collection.class.isAssignableFrom(a.getType())) {
					// this is a new collection
					Collection<Object> c = (Collection<Object>)a.getType().newInstance();
					a.setValue(obj, c);
					Type gt = a.getGenericType();
					if (!(gt instanceof ParameterizedType))
						throw new Exception("Collection " + name + " is not parameterized in class " + type.getName());
					gt = ((ParameterizedType)gt).getActualTypeArguments()[0];
					if (!(gt instanceof Class))
						throw new Exception(
							"Collection " + name + " is incorrectly parameterized in class " + type.getName());
					col = new Triple<>(c,(Class<?>)gt,a);
					collections.put(name, col);
				} else {
					// this is an object
					Object o = deserializeObject(a.getType(), a, obj, reader, rules);
					a.setValue(obj, o);
				}
			}
			if (col != null) {
				// collection element
				Object element = deserializeObject(col.getValue2(), col.getValue3(), containerInstance, reader, rules);
				col.getValue1().add(element);
			}
		}
		return obj;
	}
	
	private static void deserializeAttributes(XMLStreamCursor reader, Object object, ArrayList<Attribute> attributes) throws Exception {
		for (Pair<UnprotectedStringBuffer,UnprotectedStringBuffer> attr : reader.attributes) {
			String name = attr.getValue1().toString();
			String value = attr.getValue2().toString();
			setValue(object, name, value, attributes);
		}
	}
	
	private static void setValue(Object obj, String name, String value, ArrayList<Attribute> attributes) throws Exception {
		Attribute a = SerializationUtil.getAttributeByName(attributes, name);
		if (a == null)
			throw new Exception("Unknown attribute '" + name + "' in class " + obj.getClass().getName());
		if (!a.canSet())
			throw new Exception("Attribute '" + name + "' in class " + obj.getClass().getName() + " cannot be set");
		Object val = parseValue(value, a.getType());
		a.setValue(obj, val);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object parseValue(String s, Class<?> type) throws Exception {
		if (String.class.equals(type))
			return s;
		if ("null".equals(s))
			return null;
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return Boolean.valueOf(s);
		if (char.class.equals(type) || Character.class.equals(type))
			return s.length() > 0 ? Character.valueOf(s.charAt(0)) : null;
		if (short.class.equals(type) || Short.class.equals(type))
			return Short.valueOf(Short.parseShort(s));
		if (int.class.equals(type) || Integer.class.equals(type))
			return Integer.valueOf(Integer.parseInt(s));
		if (long.class.equals(type) || Long.class.equals(type))
			return Long.valueOf(Long.parseLong(s));
		if (float.class.equals(type) || Float.class.equals(type))
			return new Float(Float.parseFloat(s));
		if (double.class.equals(type) || Double.class.equals(type))
			return new Double(Double.parseDouble(s));
		if (type.isEnum()) {
			if (s.length() == 0) return null;
			return Enum.valueOf((Class<? extends Enum>)type, s);
		}
		throw new Exception("Cannot convert a string into " + type.getName());
	}
	
	
}
