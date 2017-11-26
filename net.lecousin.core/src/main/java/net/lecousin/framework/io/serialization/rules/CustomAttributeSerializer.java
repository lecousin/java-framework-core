package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

/** Rule to customize the serialization of an attribute. */
public class CustomAttributeSerializer implements SerializationRule {
	
	/** Constructor. */
	public CustomAttributeSerializer(Class<?> type, String name, CustomSerializer serializer) {
		this.type = type;
		this.name = name;
		this.serializer = serializer;
	}
	
	private Class<?> type;
	private String name;
	private CustomSerializer serializer;
	
	@Override
	public void apply(SerializationClass type, Object containerInstance) {
		if (!this.type.isAssignableFrom(type.getType().getBase()))
			return;
		Attribute a = type.getAttributeByOriginalName(name);
		if (a == null || !this.type.equals(a.getDeclaringClass()))
			return;
		if ((a instanceof CustomAttribute) && ((CustomAttribute)a).getCustomSerializer().getClass().equals(serializer.getClass()))
			return;
		CustomAttribute ca = new CustomAttribute(a, serializer);
		type.replaceAttribute(a, ca);
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof CustomAttributeSerializer)) return false;
		CustomAttributeSerializer r = (CustomAttributeSerializer)rule;
		return r.type.equals(type) && r.name.equals(name) && r.serializer.getClass().equals(serializer.getClass());
	}
	
	public static class CustomAttribute extends Attribute {
		
		public CustomAttribute(Attribute a, CustomSerializer serializer) {
			super(a);
			this.serializer = serializer;
			setType(serializer.targetType());
		}
		
		private CustomSerializer serializer;
		
		public CustomSerializer getCustomSerializer() {
			return serializer;
		}
		
		@Override
		public Object getValue(Object instance) throws Exception {
			Object source = super.getValue(instance);
			return serializer.serialize(source, instance);
		}
		
		@Override
		public void setValue(Object instance, Object value) throws Exception {
			super.setValue(instance, serializer.deserialize(value, instance));
		}
	}

}
