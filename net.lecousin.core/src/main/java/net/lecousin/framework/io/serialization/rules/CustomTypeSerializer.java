package net.lecousin.framework.io.serialization.rules;

import java.util.ListIterator;

import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer.CustomAttribute;

public class CustomTypeSerializer implements SerializationRule {

	public CustomTypeSerializer(CustomSerializer serializer) {
		this.serializer = serializer;
	}
	
	private CustomSerializer serializer;
	
	@Override
	public void apply(SerializationClass type) {
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.getType().equals(serializer.sourceType()))
				continue;
			it.set(new CustomAttribute(a, serializer));
		}
	}
	
}
