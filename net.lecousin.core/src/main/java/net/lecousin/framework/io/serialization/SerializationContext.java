package net.lecousin.framework.io.serialization;

import java.util.Collection;
import java.util.Iterator;

import net.lecousin.framework.collections.ArrayIterator;
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
		
		public ObjectContext(SerializationContext parent, Object instance, SerializationClass clazz, TypeDefinition originalType) {
			super(parent);
			this.instance = instance;
			this.clazz = clazz;
			this.originalType = originalType;
		}
		
		private Object instance;
		private SerializationClass clazz;
		private TypeDefinition originalType;
		
		public Object getInstance() {
			return instance;
		}
		
		public void setInstance(Object instance) {
			this.instance = instance;
		}
		
		public SerializationClass getSerializationClass() {
			return clazz;
		}
		
		public void setSerializationClass(SerializationClass clazz) {
			this.clazz = clazz;
		}
		
		public TypeDefinition getOriginalType() {
			return originalType;
		}
		
		public void setOriginalType(TypeDefinition originalType) {
			this.originalType = originalType;
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
		
		public CollectionContext(SerializationContext parent, Object col, TypeDefinition colType, TypeDefinition elementType) {
			super(parent);
			this.col = col;
			this.colType = colType;
			this.elementType = elementType;
		}
		
		private Object col;
		private TypeDefinition colType;
		private TypeDefinition elementType;
		
		public Object getCollection() {
			return col;
		}
		
		@SuppressWarnings("rawtypes")
		public Iterator<?> getIterator() {
			if (Collection.class.isAssignableFrom(col.getClass()))
				return ((Collection)col).iterator();
			return new ArrayIterator.Generic(col);
		}
		
		public TypeDefinition getCollectionType() {
			return colType;
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
