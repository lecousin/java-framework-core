package net.lecousin.framework.io.serialization.rules;

import java.lang.reflect.Method;
import java.util.List;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.util.ClassUtil;

/** Add a custom attribute to a class. */
public class AddAttributeToType implements SerializationRule {

	/** Constructor. */
	public AddAttributeToType(
		Class<?> type, String attributeName, String serializingMethodName, String deserializingMethodName
	) {
		this.type = type;
		this.attributeName = attributeName;
		this.serializingMethodName = serializingMethodName;
		this.deserializingMethodName = deserializingMethodName;
	}
	
	private Class<?> type;
	private String attributeName;
	private String serializingMethodName;
	private String deserializingMethodName;
	
	@Override
	public boolean apply(
		SerializationClass sc, SerializationContext context, List<SerializationRule> rules, boolean serializing
	) throws Exception {
		if (!type.isAssignableFrom(sc.getType().getBase()))
			return false;
		TypeDefinition t = null;
		Method sm = null;
		Method dm = null;
		if (serializingMethodName != null && !serializingMethodName.isEmpty()) {
			sm = type.getMethod(serializingMethodName);
			t = new TypeDefinition(sc.getType(), sm.getGenericReturnType());
		}
		if (deserializingMethodName != null && !deserializingMethodName.isEmpty()) {
			dm = ClassUtil.getMethod(type, deserializingMethodName, 1);
			if (dm == null)
				throw new NoSuchMethodException(
					"Deserialization method " + deserializingMethodName + " does not exist on class " + type.getName());
			if (t == null)
				t = new TypeDefinition(sc.getType(), dm.getGenericParameterTypes()[0]);
		}
		sc.getAttributes().add(new AddedAttribute(sc, t, sm, dm));
		return false;
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof AddAttributeToType)) return false;
		AddAttributeToType r = (AddAttributeToType)rule;
		return r.type.equals(type) &&
			r.attributeName.equals(attributeName) &&
			r.serializingMethodName.equals(serializingMethodName) &&
			r.deserializingMethodName.equals(deserializingMethodName);
	}
	
	private class AddedAttribute extends Attribute {
		public AddedAttribute(SerializationClass parent, TypeDefinition type, Method sm, Method dm) {
			super(parent, attributeName, type);
			this.getter = sm;
			this.setter = dm;
		}

		@Override
		public Class<?> getDeclaringClass() {
			return AddAttributeToType.this.type;
		}
		
	}
	
}
