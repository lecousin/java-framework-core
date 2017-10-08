package net.lecousin.framework.io.serialization.rules;

import java.util.ArrayList;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;

/** Specify a rule to customize serialization of an attribute. */
public class CustomAttributeSerialization implements SerializationRule {

	/** Constructor. */
	public CustomAttributeSerialization(Class<?> cl, String name, CustomAttributeSerializer<?, ?> serializer) {
		this.cl = cl;
		this.name = name;
		this.serializer = serializer;
	}
	
	private Class<?> cl;
	private String name;
	private CustomAttributeSerializer<?, ?> serializer;
	
	@Override
	public void apply(ArrayList<Attribute> attributes) {
		for (ListIterator<Attribute> it = attributes.listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!cl.equals(a.getDeclaringClass())) continue;
			if (!name.equals(a.getOriginalName())) continue;
			it.set(new CustomAttribute(a, serializer));
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class CustomAttribute extends Attribute {
		public CustomAttribute(Attribute a, CustomAttributeSerializer serializer) {
			super(a);
			this.serializer = serializer;
			type = serializer.targetType(); 
		}
		
		private CustomAttributeSerializer serializer;
		
		@SuppressWarnings("unchecked")
		@Override
		public Object getValue(Object instance) throws Exception {
			Object val = super.getValue(instance);
			return serializer.serialize(val);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void setValue(Object instance, Object value) throws Exception {
			super.setValue(instance, serializer.deserialize(value));
		}
	}
	
}
