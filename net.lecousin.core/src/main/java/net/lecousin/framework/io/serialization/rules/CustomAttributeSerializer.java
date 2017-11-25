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
	public void apply(SerializationClass type) {
		if (!this.type.isAssignableFrom(type.getType().getBase()))
			return;
		Attribute a = type.getAttributeByOriginalName(name);
		if (a == null || !this.type.equals(a.getDeclaringClass()))
			return;
		CustomAttribute ca = new CustomAttribute(a, serializer);
		type.replaceAttribute(a, ca);
	}
	
	public static class CustomAttribute extends Attribute {
		
		public CustomAttribute(Attribute a, CustomSerializer serializer) {
			super(a);
			this.serializer = serializer;
			setType(serializer.targetType());
		}
		
		private CustomSerializer serializer;
		
		@Override
		public Object getValue(Object instance) throws Exception {
			Object source = super.getValue(instance);
			return serializer.serialize(source);
		}
		
		@Override
		public void setValue(Object instance, Object value) throws Exception {
			super.setValue(instance, serializer.deserialize(value));
		}
	}

}
