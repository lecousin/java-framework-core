package net.lecousin.framework.io.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.io.serialization.annotations.AnnotationToRule;
import net.lecousin.framework.io.serialization.annotations.TypeSerializer;
import net.lecousin.framework.io.serialization.rules.CustomSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Utility methods for serialization. */
public final class SerializationUtil {
	
	private SerializationUtil() { /* no instance */ }

	/** Represent an attribute of a class, with getter, setter, original name, type, etc.. */
	public static class Attribute {
		protected String name;
		protected String originalName;
		protected Field field;
		protected Method getter;
		protected Method setter;
		protected Class<?> type;
		protected Type genericType;
		protected boolean noSerialization;
		
		/** Constructor. */
		public Attribute(String name, Field field, Method getter, Method setter, Class<?> type, Type genericType) {
			this.name = name;
			this.originalName = name;
			this.field = field;
			this.getter = getter;
			this.setter = setter;
			this.type = type;
			this.genericType = genericType;
		}

		/** Constructor. */
		protected Attribute(Attribute a) {
			this.name = a.name;
			this.originalName = a.originalName;
			this.field = a.field;
			this.getter = a.getter;
			this.setter = a.setter;
			this.type = a.type;
			this.genericType = a.genericType;
			this.noSerialization = a.noSerialization;
		}
		
		public String getOriginalName() { return originalName; }
		
		public String getName() { return name; }
		
		public void rename(String newName) { name = newName; }
		
		public Class<?> getType() { return type; }
		
		public Type getGenericType() { return genericType; }
		
		public void ignoreSerialization() { noSerialization = true; }
		
		/** Return true if a getter exists or the field is public. */
		public boolean canGet() {
			if (getter != null)
				return true;
			if (field != null && (field.getModifiers() & Modifier.PUBLIC) != 0)
				return true;
			return false;
		}

		/** Return true if a setter exists or the field is public. */
		public boolean canSet() {
			if (setter != null)
				return true;
			if (field != null && (field.getModifiers() & Modifier.PUBLIC) != 0)
				return true;
			return false;
		}
		
		/** Return the declaring class of the field or the getter or the setter. */
		public Class<?> getDeclaringClass() {
			if (field != null)
				return field.getDeclaringClass();
			if (getter != null)
				return getter.getDeclaringClass();
			return setter.getDeclaringClass();
		}
		
		/** Return the value of this attribute for the given instance. */
		public Object getValue(Object instance) throws Exception {
			Object val;
			if (getter != null)
				val = getter.invoke(instance);
			else
				val = field.get(instance);
			return val;
		}
		
		/** Set the value of this attribute for the given instance. */
		public void setValue(Object instance, Object value) throws Exception {
			if (setter != null)
				setter.invoke(instance, value);
			else
				field.set(instance, value);
		}
		
		/** Instantiate a new object of the type of this attribute. */
		public Object instantiate(@SuppressWarnings("unused") Object containerInstance) throws Exception {
			if (Collection.class.isAssignableFrom(type) && genericType instanceof ParameterizedType) {
				Type t = ((ParameterizedType)genericType).getActualTypeArguments()[0];
				if (t instanceof Class)
					return ((Class<?>)t).newInstance();
			}
			return type.newInstance();
		}
		
		/** Retrieve a specific annotation. */
		public <T extends Annotation> T getAnnotation(boolean onGet, Class<T> annotationType) {
			if (field != null) {
				T a = field.getAnnotation(annotationType);
				if (a != null)
					return a;
			}
			if (onGet && getter != null) {
				T a = getter.getAnnotation(annotationType);
				if (a != null)
					return a;
			}
			if (!onGet && setter != null) {
				T a = setter.getAnnotation(annotationType);
				if (a != null)
					return a;
			}
			return null;
		}
	}
	
	/** Return the attributes with getter and setter for the given class. */
	public static ArrayList<Attribute> getAttributes(Class<?> cl) {
		ArrayList<Attribute> attributes = new ArrayList<>();
		Class<?> c = cl;
		do {
			getAttributes(c, attributes);
			c = c.getSuperclass();
			if (c == null) break;
			if (Object.class.equals(c)) break;
		} while (true);
		c = cl;
		do {
			getAttributesFromInterfaces(c, attributes);
			c = c.getSuperclass();
			if (c == null) break;
			if (Object.class.equals(c)) break;
		} while (true);
		return attributes;
	}
	
	private static void getAttributes(Class<?> c, List<Attribute> attributes) {
		// build attributes from fields
		for (Field f : c.getDeclaredFields()) {
			String name = f.getName();
			Attribute a = getAttributeByOriginalName(attributes, name);
			if (a != null && a.field != null)
				continue; // we have an attribute from upper class, overriding it
			
			int m = f.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC | Modifier.TRANSIENT);
			
			if ((m & Modifier.STATIC) != 0)
				continue; // skip static field
			
			if ((m & Modifier.TRANSIENT) != 0) {
				// the field is transient, we must ignore it for serialization
				if (a != null) {
					a.field = f;
					a.noSerialization = true;
				} else {
					a = new Attribute(name, f, null, null, f.getType(), f.getGenericType());
					a.noSerialization = true;
					attributes.add(a);
				}
				continue;
			}
			
			if (a != null)
				a.field = f;
			else {
				a = new Attribute(name, f, null, null, f.getType(), f.getGenericType());
				attributes.add(a);
			}
		}
		// search for getters and setters
		for (Method m : c.getDeclaredMethods()) {
			if ((m.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) != Modifier.PUBLIC)
				continue; // skip non-public or static methods
			
			Class<?>[] paramsTypes = m.getParameterTypes();
			if (paramsTypes.length == 0) {
				// may be a getter
				Class<?> type = m.getReturnType();
				if (Void.class.equals(type))
					continue; // a getter must return a value
				
				String name = m.getName();
				if (name.startsWith("get")) {
					if (name.length() == 3)
						continue;
					name = name.substring(3, 4).toLowerCase() + (name.length() > 3 ? name.substring(4) : "");
				} else if (name.startsWith("is")) {
					if (!boolean.class.equals(type) && !Boolean.class.equals(type))
						continue;
					if (name.length() == 2)
						continue;
					name = name.substring(2, 3).toLowerCase() + (name.length() > 2 ? name.substring(3) : "");
				} else
					continue;
				
				Attribute a = getAttributeByOriginalName(attributes, name);
				if (a != null) {
					if (a.getter != null)
						continue; // a getter overrides it in an upper class
					if (!type.isAssignableFrom(a.type))
						continue; // not compatible
					a.getter = m;
				} else {
					a = new Attribute(name, null, m, null, type, m.getGenericReturnType());
					attributes.add(a);
				}
			} else if (paramsTypes.length == 1) {
				// may be a setter
				String name = m.getName();
				if (name.startsWith("set")) {
					if (name.length() == 3)
						continue;
					name = name.substring(3, 4).toLowerCase() + (name.length() > 3 ? name.substring(4) : "");
				} else
					continue;

				Attribute a = getAttributeByOriginalName(attributes, name);
				if (a != null) {
					if (a.setter != null)
						continue; // a setter overrides it in an upper class
					if (!a.type.isAssignableFrom(paramsTypes[0]))
						continue; // not compatible
					a.setter = m;
				} else {
					a = new Attribute(name, null, null, m, paramsTypes[0], m.getGenericParameterTypes()[0]);
					attributes.add(a);
				}
			}
		}
	}
	
	private static void getAttributesFromInterfaces(Class<?> c, List<Attribute> attributes) {
		for (Class<?> i : c.getInterfaces()) {
			getAttributes(i, attributes);
			getAttributesFromInterfaces(i, attributes);
		}
	}
	
	/** Search an attribute by its original name. */
	public static Attribute getAttributeByOriginalName(List<Attribute> attributes, String name) {
		for (Attribute a : attributes)
			if (name.equals(a.originalName))
				return a;
		return null;
	}
	
	/** Search an attribute by its name. */
	public static Attribute getAttributeByName(List<Attribute> attributes, String name) {
		for (Attribute a : attributes)
			if (name.equals(a.name))
				return a;
		return null;
	}
	
	/** Remove attributes that should not be serialized. */
	public static void removeIgnoredAttributes(List<Attribute> attributes) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); )
			if (it.next().noSerialization)
				it.remove();
	}
	
	/** Remove attributes that cannot be retrieved. */
	public static void removeNonGettableAttributes(List<Attribute> attributes) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); )
			if (!it.next().canGet())
				it.remove();
	}
	
	/** Remove attributes that cannot be set. */
	public static void removeNonSettableAttributes(List<Attribute> attributes) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); )
			if (!it.next().canSet())
				it.remove();
	}

	/** Analyze annotations of attributes, and create corresponding rules. */
	public static List<SerializationRule> processAnnotations(List<Attribute> attributes, boolean onGetter, boolean onSetter) {
		ArrayList<SerializationRule> rules = new ArrayList<>();
		for (Attribute a : attributes) {
			if (a.field != null)
				for (Annotation an : a.field.getAnnotations()) {
					SerializationRule rule = AnnotationToRule.getRule(a, an);
					if (rule != null)
						rules.add(rule);
				}
			if (onGetter && a.getter != null) {
				for (Annotation an : a.getter.getAnnotations()) {
					SerializationRule rule = AnnotationToRule.getRule(a, an);
					if (rule != null)
						rules.add(rule);
				}
			}
			if (onSetter && a.setter != null)
				for (Annotation an : a.setter.getAnnotations()) {
					SerializationRule rule = AnnotationToRule.getRule(a, an);
					if (rule != null)
						rules.add(rule);
				}
		}
		return rules;
	}
	
	/** Check if TypeSerializer annotations are present on the given type, or its super-class and interfaces, and return a new list. */
	public static List<CustomSerializer<?,?>> getNewSerializers(List<CustomSerializer<?,?>> currentList, Class<?> type)
	throws IllegalAccessException, InstantiationException {
		LinkedList<CustomSerializer<?,?>> list = new LinkedList<>();
		getCustomSerializers(type, list);
		if (list.isEmpty())
			return currentList;
		ArrayList<CustomSerializer<?,?>> newList = new ArrayList<>(list.size() + currentList.size());
		newList.addAll(list);
		newList.addAll(currentList);
		return newList;
	}

	/** Check if TypeSerializer annotations are present on the given attribute, and return a new list. */
	public static List<CustomSerializer<?,?>> getNewSerializers(List<CustomSerializer<?,?>> currentList, Attribute attr)
	throws IllegalAccessException, InstantiationException {
		LinkedList<CustomSerializer<?,?>> list = new LinkedList<>();
		if (attr.field != null) {
			for (TypeSerializer s : attr.field.getAnnotationsByType(TypeSerializer.class))
				list.add(s.value().newInstance());
		}
		if (list.isEmpty())
			return currentList;
		ArrayList<CustomSerializer<?,?>> newList = new ArrayList<>(list.size() + currentList.size());
		newList.addAll(list);
		newList.addAll(currentList);
		return newList;
	}
	
	private static void getCustomSerializers(Class<?> type, LinkedList<CustomSerializer<?,?>> list)
	throws IllegalAccessException, InstantiationException {
		for (Class<?> i : type.getInterfaces())
			getCustomSerializers(i, list);
		if (type.getSuperclass() != null)
			getCustomSerializers(type.getSuperclass(), list);
		TypeSerializer[] serializers = type.getAnnotationsByType(TypeSerializer.class);
		for (TypeSerializer s : serializers) {
			CustomSerializer<?,?> found = null;
			for (CustomSerializer<?,?> cs : list)
				if (cs.getClass().equals(s.value())) {
					found = cs;
					break;
				}
			if (found != null) {
				// put it in front
				list.remove(found);
				list.addFirst(found);
			} else
				list.addFirst(s.value().newInstance());
		}
	}
	
	/** Search for a custom serializer. */
	public static CustomSerializer<?,?> getCustomSerializer(Class<?> type, List<CustomSerializer<?,?>> serializers) {
		for (CustomSerializer<?,?> s : serializers)
			if (s.sourceType().equals(type))
				return s;
		return null;
	}
	
	/** Instantiate a collection type. */
	@SuppressWarnings("unchecked")
	public static Collection<Object> instantiateCollection(Class<?> collectionType) throws IllegalAccessException, InstantiationException {
		if ((collectionType.getModifiers() & Modifier.ABSTRACT) == 0 && !collectionType.isInterface())
			return (Collection<Object>)collectionType.newInstance();
		if (collectionType.isAssignableFrom(ArrayList.class))
			return new ArrayList<>();
		if (collectionType.isAssignableFrom(LinkedList.class))
			return new LinkedList<>();
		if (collectionType.isAssignableFrom(HashSet.class))
			return new HashSet<>();
		throw new InstantiationException("We don't know how to instantiate a collection of type " + collectionType.getName());
	}
	
}
