package net.lecousin.framework.io.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.serialization.rules.TypeFactory;

public class SerializationClass {
	
	public SerializationClass(TypeDefinition type) {
		this.type = type;
		populateAttributes();
	}
	
	private TypeDefinition type;
	private List<Attribute> attributes = new ArrayList<>();
	
	public TypeDefinition getType() {
		return type;
	}
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	public Attribute getAttributeByName(String name) {
		for (Attribute a : attributes)
			if (name.equals(a.getName()))
				return a;
		return null;
	}
	
	public Attribute getAttributeByOriginalName(String name) {
		for (Attribute a : attributes)
			if (name.equals(a.getOriginalName()))
				return a;
		return null;
	}
	
	public void replaceAttribute(Attribute original, Attribute newAttribute) {
		for (ListIterator<Attribute> it = attributes.listIterator(); it.hasNext(); )
			if (it.next().equals(original)) {
				it.set(newAttribute);
				break;
			}
	}
	
	public void apply(List<SerializationRule> rules, Object containerInstance) {
		for (SerializationRule rule : rules)
			rule.apply(this, containerInstance);
	}
	
	public static class Attribute {
		
		public Attribute(String name, TypeDefinition type) {
			this.name = this.originalName = name;
			this.type = this.originalType = type;
		}
		
		public Attribute(Field f) {
			this.name = this.originalName = f.getName();
			this.type = this.originalType = new TypeDefinition(f.getGenericType());
			if ((f.getModifiers() & Modifier.TRANSIENT) != 0) ignore = true;
		}
		
		public Attribute(Attribute copy) {
			this.name = copy.name;
			this.originalName = copy.originalName;
			this.field = copy.field;
			this.getter = copy.getter;
			this.setter = copy.setter;
			this.type = copy.type;
			this.originalType = copy.originalType;
			this.ignore = copy.ignore;
		}
		
		private String name;
		private String originalName;
		private Field field;
		private Method getter;
		private Method setter;
		private TypeDefinition type;
		private TypeDefinition originalType;
		private boolean ignore = false;
		
		public String getName() {
			return name;
		}
		
		public String getOriginalName() {
			return originalName;
		}
		
		public Field getField() {
			return field;
		}
		
		public Method getGetter() {
			return getter;
		}
		
		public Method getSetter() {
			return setter;
		}
		
		public TypeDefinition getType() {
			return type;
		}
		
		public TypeDefinition getOriginalType() {
			return originalType;
		}
		
		public boolean ignore() {
			return ignore;
		}
		
		public void renameTo(String newName) {
			this.name = newName;
		}
		
		public void setType(TypeDefinition type) {
			this.type = type;
		}
		
		public void ignore(boolean ignore) {
			this.ignore = ignore;
		}
		
		/** Return the declaring class of the field or the getter or the setter. */
		public Class<?> getDeclaringClass() {
			if (field != null)
				return field.getDeclaringClass();
			if (getter != null)
				return getter.getDeclaringClass();
			return setter.getDeclaringClass();
		}
		
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
			return instantiate(type.getBase());
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
		
		/** Retrieve all annotations. */
		public List<Annotation> getAnnotations(boolean onGet) {
			LinkedList<Annotation> list = new LinkedList<>();
			if (field != null)
				Collections.addAll(list, field.getAnnotations());
			if (onGet && getter != null)
				Collections.addAll(list, getter.getAnnotations());
			if (!onGet && setter != null)
				Collections.addAll(list, setter.getAnnotations());
			return list;
		}
	}
	
	private void populateAttributes() {
		populateAttributes(type.getBase());
		filterAttributes();
	}
	
	private void populateAttributes(Class<?> type) {
		for (Field f : type.getDeclaredFields()) {
			String name = f.getName();
			Attribute a = getAttributeByOriginalName(name);
			if (a == null) {
				a = new Attribute(f);
				attributes.add(a);
			}
		}
		for (Method m : type.getDeclaredMethods()) {
			if ((m.getModifiers() & Modifier.PUBLIC) == 0) continue;
			String name = m.getName();
			if (name.startsWith("get")) {
				if (name.length() == 3) continue;
				name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
				if (m.getParameterCount() != 0) continue;
				Class<?> returnType = m.getReturnType();
				if (returnType == null || Void.class.equals(returnType) || void.class.equals(returnType)) continue;
				Attribute a = getAttributeByOriginalName(name);
				if (a == null)
					a = new Attribute(name, new TypeDefinition(m.getGenericReturnType()));
				if (a.getter == null)
					a.getter = m;
			} else if (name.startsWith("is")) {
				if (name.length() == 2) continue;
				name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
				if (m.getParameterCount() != 0) continue;
				Class<?> returnType = m.getReturnType();
				if (returnType == null || Void.class.equals(returnType) || void.class.equals(returnType)) continue;
				if (!returnType.equals(boolean.class) && !returnType.equals(Boolean.class)) continue;
				Attribute a = getAttributeByOriginalName(name);
				if (a == null)
					a = new Attribute(name, new TypeDefinition(returnType));
				if (a.getter == null)
					a.getter = m;
			} else if (name.startsWith("set")) {
				if (name.length() == 3) continue;
				name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
				if (m.getParameterCount() != 1) continue;
				Attribute a = getAttributeByOriginalName(name);
				if (a == null)
					a = new Attribute(name, new TypeDefinition(m.getGenericParameterTypes()[0]));
				if (a.setter == null)
					a.setter = m;
			}
		}
		if (type.getSuperclass() != null)
			populateAttributes(type.getSuperclass());
	}
	
	private void filterAttributes() {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.canGet() && !a.canSet())
				it.remove();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Object instantiate(Class<?> type) throws Exception {
		if (type.isAssignableFrom(ArrayList.class))
			return new ArrayList();
		if (type.isAssignableFrom(LinkedList.class))
			return new LinkedList();
		if (type.isAssignableFrom(HashSet.class))
			return new HashSet();
		if (type.isAssignableFrom(HashMap.class))
			return new HashMap();
		return type.newInstance();
	}
	
	public static Object instantiate(Class<?> type, List<SerializationRule> rules) throws Exception {
		Object instance = null;
		for (SerializationRule rule : rules)
			if (rule instanceof TypeFactory) {
				TypeFactory<?> factory = (TypeFactory<?>)rule;
				if (type.isAssignableFrom(factory.getType())) {
					instance = factory.getFactory().provide();
					if (instance != null)
						break;
				}
			}
		if (instance == null)
			instance = instantiate(type);
		for (SerializationRule rule : rules)
			if (rule instanceof SerializationRule.DeserializationInstanceListener)
				((SerializationRule.DeserializationInstanceListener)rule).onInstantiation(instance);
		return instance;
	}
	
}
