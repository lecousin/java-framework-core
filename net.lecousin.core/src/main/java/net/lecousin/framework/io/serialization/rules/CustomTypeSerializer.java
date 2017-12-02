package net.lecousin.framework.io.serialization.rules;

import java.util.ListIterator;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer.CustomAttribute;

public class CustomTypeSerializer implements SerializationRule {

	public CustomTypeSerializer(CustomSerializer serializer) {
		this.serializer = serializer;
	}
	
	private CustomSerializer serializer;
	
	@Override
	public void apply(SerializationClass type, SerializationContext context) {
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.getType().equals(serializer.sourceType()))
				continue;
			if ((a instanceof CustomAttribute) && ((CustomAttribute)a).getCustomSerializer().getClass().equals(serializer.getClass()))
				continue;
			it.set(new CustomAttribute(a, serializer));
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof CustomTypeSerializer)) return false;
		CustomTypeSerializer r = (CustomTypeSerializer)rule;
		return r.serializer.getClass().equals(serializer.getClass());
	}
	
}
