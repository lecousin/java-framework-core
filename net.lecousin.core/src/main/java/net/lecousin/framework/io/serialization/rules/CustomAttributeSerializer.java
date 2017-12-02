package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.io.serialization.SerializationContext;

/** Rule to customize the serialization of an attribute. */
public class CustomAttributeSerializer implements SerializationRule {
	
	/** Constructor. */
	public CustomAttributeSerializer(OnClassAttribute pattern, CustomSerializer serializer) {
		this.pattern = pattern;
		this.serializer = serializer;
	}

	/** Constructor. */
	public CustomAttributeSerializer(Class<?> type, String name, CustomSerializer serializer) {
		this(new OnClassAttribute(type, name), serializer);
	}
	
	private OnClassAttribute pattern;
	private CustomSerializer serializer;
	
	@Override
	public void apply(SerializationClass type, SerializationContext context) {
		Attribute a = pattern.getAttribute(type, context);
		if (a == null)
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
		return r.pattern.isEquivalent(pattern) && r.serializer.getClass().equals(serializer.getClass());
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
