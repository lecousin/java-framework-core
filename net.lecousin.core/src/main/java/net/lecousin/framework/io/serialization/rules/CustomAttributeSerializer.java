package net.lecousin.framework.io.serialization.rules;

import java.util.List;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.io.serialization.SerializationException;

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
	@SuppressWarnings("squid:S3516") // always return false
	public boolean apply(SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing) {
		Attribute a = pattern.getAttribute(type, context);
		if (a == null)
			return false;
		if ((a instanceof CustomAttribute) && ((CustomAttribute)a).getCustomSerializer().getClass().equals(serializer.getClass()))
			return false;
		CustomAttribute ca = new CustomAttribute(a, serializer);
		type.replaceAttribute(a, ca);
		return false;
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof CustomAttributeSerializer)) return false;
		CustomAttributeSerializer r = (CustomAttributeSerializer)rule;
		return r.pattern.isEquivalent(pattern) && r.serializer.getClass().equals(serializer.getClass());
	}
	
	/** Used to override an attribute with a custom serializer. */
	public static class CustomAttribute extends Attribute {
		
		/** Constructor. */
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
		public Object getValue(Object instance) throws SerializationException {
			Object source = super.getValue(instance);
			return serializer.serialize(source, instance);
		}
		
		@Override
		public void setValue(Object instance, Object value) throws SerializationException {
			super.setValue(instance, serializer.deserialize(value, instance));
		}
	}

}
