package net.lecousin.framework.io.serialization;

import java.util.Collection;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

public abstract class SerializationContext {

	public SerializationContext(SerializationContext parent) {
		this.parent = parent;
	}
	
	protected SerializationContext parent;
	
	public SerializationContext getParent() {
		return parent;
	}
	
	public abstract ObjectContext getContainerObjectContext();
	
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
		
		@Override
		public ObjectContext getContainerObjectContext() {
			return this;
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
		
		@Override
		public ObjectContext getContainerObjectContext() {
			return getParent();
		}
		
	}
	
	public static class CollectionContext extends SerializationContext {
		
		public CollectionContext(SerializationContext parent, Collection<?> col, TypeDefinition elementType) {
			super(parent);
			this.col = col;
			this.elementType = elementType;
		}
		
		private Collection<?> col;
		private TypeDefinition elementType;
		
		public Collection<?> getCollection() {
			return col;
		}
		
		public TypeDefinition getElementType() {
			return elementType;
		}
		
		@Override
		public ObjectContext getContainerObjectContext() {
			return getParent().getContainerObjectContext();
		}
		
	}
	
}
