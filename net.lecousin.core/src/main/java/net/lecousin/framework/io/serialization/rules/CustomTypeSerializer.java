package net.lecousin.framework.io.serialization.rules;

import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer.CustomAttribute;

/**
 * Rule that specifies a custom serializer.
 * It can be applied to a specific attribute only by specifying a OnClassAttribute context,
 * to a class only by specifying a OnClass context, or generally by not specifying a context.
 * Each time an attribute is found with the same type as the custom serializer's sourceType,
 * and the context matches, the custom serializer will be used instead of the default one.
 */
public class CustomTypeSerializer implements SerializationRule {

	/** Constructor. */
	public CustomTypeSerializer(CustomSerializer serializer, SerializationContextPattern context) {
		this.serializer = serializer;
		this.context = context;
	}
	
	private CustomSerializer serializer;
	private SerializationContextPattern context;
	
	@Override
	@SuppressWarnings("squid:S3516") // always return false
	public boolean apply(SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing) {
		if (this.context != null && !this.context.matches(type, context))
			return false;
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (this.context != null && !this.context.matches(type, context, a))
				continue;
			if (!a.getType().equals(serializer.sourceType()))
				continue;
			if ((a instanceof CustomAttribute) && ((CustomAttribute)a).getCustomSerializer().getClass().equals(serializer.getClass()))
				continue;
			it.set(new CustomAttribute(a, serializer));
		}
		return false;
	}
	
	@Override
	public Object convertSerializationValue(Object value, TypeDefinition type, SerializationContext context) throws SerializationException {
		if (!type.equals(serializer.sourceType()))
			return value;
		if (this.context != null && !this.context.matches(context))
			return value;
		return serializer.serialize(value, context.getContainerObjectContext().getInstance());
	}
	
	@Override
	public TypeDefinition getDeserializationType(TypeDefinition type, SerializationContext context) {
		if (!type.equals(serializer.sourceType()))
			return type;
		if (this.context != null && !this.context.matches(context))
			return type;
		return serializer.targetType();
	}
	
	@Override
	public Object getDeserializationValue(Object value, TypeDefinition type, SerializationContext context) throws SerializationException {
		if (!type.equals(serializer.sourceType()))
			return value;
		if (this.context != null && !this.context.matches(context))
			return value;
		return serializer.deserialize(value, context.getContainerObjectContext().getInstance());
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof CustomTypeSerializer)) return false;
		CustomTypeSerializer r = (CustomTypeSerializer)rule;
		return r.serializer.getClass().equals(serializer.getClass());
	}
	
}
