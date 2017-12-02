package net.lecousin.framework.io.serialization;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

public abstract class SerializationContext {

	public SerializationContext(SerializationContext parent) {
		this.parent = parent;
	}
	
	protected SerializationContext parent;
	
	public SerializationContext getParent() {
		return parent;
	}
	
	public static class ObjectContext extends SerializationContext {
		
		public ObjectContext(SerializationContext parent, Object instance, SerializationClass clazz) {
			super(parent);
			this.instance = instance;
			this.clazz = clazz;
		}
		
		private Object instance;
		private SerializationClass clazz;
		
		public Object getInstance() {
			return instance;
		}
		
		public SerializationClass getSerializationClass() {
			return clazz;
		}
		
	}
	
	public static class AttributeContext extends SerializationContext {
		
		public AttributeContext(ObjectContext parent, Attribute attribute) {
			super(parent);
			this.attribute = attribute;
		}
		
		private Attribute attribute;
		
		@Override
		public ObjectContext getParent() {
			return (ObjectContext)super.getParent();
		}
		
		public Attribute getAttribute() {
			return attribute;
		}
		
	}
	
}
