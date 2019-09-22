package net.lecousin.framework.io.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Represent a class during serialization process, with attributes.
 * This can be used by rules to customize the serialization.
 */
public class SerializationClass {
	
	/** Constructor. */
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
	
	/** Get an attribute by its name. */
	public Attribute getAttributeByName(String name) {
		for (Attribute a : attributes)
			if (name.equals(a.getName()))
				return a;
		return null;
	}
	
	/** Get an attribute by its original name. */
	public Attribute getAttributeByOriginalName(String name) {
		for (Attribute a : attributes)
			if (name.equals(a.getOriginalName()))
				return a;
		return null;
	}
	
	/** Replace an attribute. */
	public void replaceAttribute(Attribute original, Attribute newAttribute) {
		for (ListIterator<Attribute> it = attributes.listIterator(); it.hasNext(); )
			if (it.next().equals(original)) {
				it.set(newAttribute);
				break;
			}
	}
	
	/** Apply rules. */
	public void apply(List<SerializationRule> rules, SerializationContext context, boolean serializing) throws SerializationException {
		for (SerializationRule rule : rules)
			if (rule.apply(this, context, rules, serializing))
				break;
	}
	
	/** Represent an attribute of the class. */
	public static class Attribute {
		
		/** Constructor. */
		public Attribute(SerializationClass parent, String name, TypeDefinition type) {
			this.parent = parent;
			this.name = this.originalName = name;
			this.type = this.originalType = type;
		}
		
		/** Constructor. */
		public Attribute(SerializationClass parent, Field f) {
			this.parent = parent;
			this.field = f;
			this.name = this.originalName = f.getName();
			this.type = this.originalType = new TypeDefinition(parent.getType(), f.getGenericType());
			if ((f.getModifiers() & Modifier.TRANSIENT) != 0) ignore = true;
		}
		
		/** Constructor. */
		public Attribute(Attribute copy) {
			this.parent = copy.parent;
			this.name = copy.name;
			this.originalName = copy.originalName;
			this.field = copy.field;
			this.getter = copy.getter;
			this.setter = copy.setter;
			this.type = copy.type;
			this.originalType = copy.originalType;
			this.ignore = copy.ignore;
		}
		
		protected SerializationClass parent;
		protected String name;
		protected String originalName;
		protected Field field;
		protected Method getter;
		protected Method setter;
		protected TypeDefinition type;
		protected TypeDefinition originalType;
		protected boolean ignore = false;
		
		public SerializationClass getParent() {
			return parent;
		}
		
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
		
		/** Return true if this attribute must be ignored during (de)serialization. */
		public boolean ignore() {
			return ignore;
		}
		
		/** Set the ignore flag. */
		public void ignore(boolean ignore) {
			this.ignore = ignore;
		}
		
		/** Rename this attribute. */
		public void renameTo(String newName) {
			this.name = newName;
		}
		
		public void setType(TypeDefinition type) {
			this.type = type;
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
			return (getter != null) ||
				(field != null && (field.getModifiers() & Modifier.PUBLIC) != 0);
		}

		/** Return true if a setter exists or the field is public. */
		public boolean canSet() {
			return (setter != null) ||
				(field != null && (field.getModifiers() & Modifier.PUBLIC) != 0);
		}
		
		/** Return the value of this attribute for the given instance. */
		public Object getValue(Object instance) throws SerializationException {
			try {
				Object val;
				if (getter != null)
					val = getter.invoke(instance);
				else
					val = field.get(instance);
				return val;
			} catch (Exception e) {
				throw new SerializationException("Error getting field " + originalName, e);
			}
		}
		
		/** Set the value of this attribute for the given instance. */
		public void setValue(Object instance, Object value) throws SerializationException {
			try {
				if (setter != null)
					setter.invoke(instance, value);
				else
					field.set(instance, value);
			} catch (Exception e) {
				throw new SerializationException("Error setting field " + originalName, e);
			}
		}
		
		/** Instantiate a new object of the type of this attribute. */
		public Object instantiate(AttributeContext context) throws SerializationException {
			try {
				return SerializationClass.instantiate(context.getAttribute().getType().getBase());
			} catch (Exception e) {
				throw new SerializationException("Error instantiating field " + originalName, e);
			}
		}
		
		/** Return true if this attribute has a custom instantiation and no type information should not be serialized. */
		public boolean hasCustomInstantiation() {
			return false;
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
		populateAttributes(type.getBase(), type.getParameters());
		filterAttributes();
	}
	
	@SuppressWarnings("rawtypes")
	private void populateAttributes(Class<?> type, List<TypeDefinition> params) {
		if (Object.class.equals(type))
			return;
		for (Field f : type.getDeclaredFields()) {
			populateFieldAttribute(f);
		}
		for (Method m : type.getDeclaredMethods()) {
			populateMethodAttribute(m, type, params);
		}
		if (type.getSuperclass() != null) {
			Type t = type.getGenericSuperclass();
			List<TypeDefinition> superParams = new LinkedList<>();
			if (t instanceof ParameterizedType) {
				for (Type arg : ((ParameterizedType)t).getActualTypeArguments()) {
					if (arg instanceof TypeVariable) {
						TypeVariable[] typeArgs = type.getTypeParameters();
						for (int i = 0; i < typeArgs.length; ++i)
							if (typeArgs[i].getName().equals(((TypeVariable)arg).getName())) {
								superParams.add(params.get(i));
								break;
							}
					} else {
						superParams.add(new TypeDefinition(null, arg));
					}
				}
			}
			populateAttributes(type.getSuperclass(), superParams);
		}
	}
	
	private void populateFieldAttribute(Field f) {
		if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) return;
		String name = f.getName();
		Attribute a = getAttributeByOriginalName(name);
		if (a == null) {
			a = new Attribute(this, f);
			attributes.add(a);
		}
	}
	
	private void populateMethodAttribute(Method m, Class<?> type, List<TypeDefinition> params) {
		if ((m.getModifiers() & Modifier.PUBLIC) == 0) return;
		String name = m.getName();
		if (name.startsWith("get")) {
			if (name.length() == 3) return;
			if (m.getParameterCount() != 0) return;
			Class<?> returnType = m.getReturnType();
			if (returnType == null || Void.class.equals(returnType) || void.class.equals(returnType)) return;
			createAttributeFromGetter(Character.toLowerCase(name.charAt(3)) + name.substring(4), m, type, params);
		} else if (name.startsWith("is")) {
			if (name.length() == 2) return;
			if (m.getParameterCount() != 0) return;
			Class<?> returnType = m.getReturnType();
			if (returnType == null || (!returnType.equals(boolean.class) && !returnType.equals(Boolean.class))) return;
			createAttributeFromGetterIs(Character.toLowerCase(name.charAt(2)) + name.substring(3), m, returnType);
		} else if (name.startsWith("set")) {
			if (name.length() == 3) return;
			if (m.getParameterCount() != 1) return;
			createAttributeFromSetter(Character.toLowerCase(name.charAt(3)) + name.substring(4), m, type, params);
		}
	}
	
	private void createAttributeFromGetter(String name, Method m, Class<?> type, List<TypeDefinition> params) {
		Attribute a = getAttributeByOriginalName(name);
		if (a == null) {
			a = new Attribute(this, name, new TypeDefinition(new TypeDefinition(type, params), m.getGenericReturnType()));
			attributes.add(a);
		}
		if (a.getter == null)
			a.getter = m;
	}

	private void createAttributeFromGetterIs(String name, Method m, Class<?> returnType) {
		Attribute a = getAttributeByOriginalName(name);
		if (a == null) {
			a = new Attribute(this, name, new TypeDefinition(returnType));
			attributes.add(a);
		}
		if (a.getter == null)
			a.getter = m;
	}
	
	private void createAttributeFromSetter(String name, Method m, Class<?> type, List<TypeDefinition> params) {
		Attribute a = getAttributeByOriginalName(name);
		if (a == null)
			a = new Attribute(this, name,
				new TypeDefinition(new TypeDefinition(type, params), m.getGenericParameterTypes()[0]));
		if (a.setter == null)
			a.setter = m;
	}
	
	private void filterAttributes() {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.canGet() && !a.canSet())
				it.remove();
		}
	}
	
	/** Search an attribute in the given type. */
	public static TypeDefinition searchAttributeType(TypeDefinition containerType, String attributeName) {
		try {
			Field f = containerType.getBase().getField(attributeName);
			if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0)
				return new TypeDefinition(containerType, f.getGenericType());
		} catch (Exception t) { /* ignore */ }
		try {
			Method m = containerType.getBase()
				.getMethod("get" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1));
			Class<?> returnType = m.getReturnType();
			if (returnType != null && !Void.class.equals(returnType) && !void.class.equals(returnType))
				return new TypeDefinition(containerType, m.getGenericReturnType());
		} catch (Exception t) { /* ignore */ }
		try {
			Method m = containerType.getBase()
				.getMethod("is" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1));
			Class<?> returnType = m.getReturnType();
			if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
				return new TypeDefinition(containerType, m.getGenericReturnType());
		} catch (Exception t) { /* ignore */ }
		return null;
	}
	
	/** Instantiate the given type. */
	@SuppressWarnings("rawtypes")
	public static Object instantiate(Class<?> type) throws ReflectiveOperationException {
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
	
	/** Instantiate the given type. */
	public static Object instantiate(
		TypeDefinition type, SerializationContext context, List<SerializationRule> rules, boolean forceType
	) throws SerializationException, ReflectiveOperationException {
		Object instance = null;
		for (SerializationRule rule : rules)
			if (rule.canInstantiate(type, context)) {
				instance = rule.instantiate(type, context);
				break;
			}
		if (instance == null) {
			if (context instanceof AttributeContext && !forceType)
				instance = ((AttributeContext)context).getAttribute().instantiate((AttributeContext)context);
			else
				instance = instantiate(type.getBase());
		}
		for (SerializationRule rule : rules)
			rule.onInstantiation(type, instance, context);
		return instance;
	}
	
}
