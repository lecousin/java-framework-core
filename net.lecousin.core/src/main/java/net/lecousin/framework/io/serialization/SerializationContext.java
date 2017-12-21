package net.lecousin.framework.io.serialization;

import java.util.Collection;
import java.util.Iterator;

import net.lecousin.framework.collections.ArrayIterator;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

/** Specify the current context during (de)serialization. */
public abstract class SerializationContext {

	/** Constructor. */
	public SerializationContext(SerializationContext parent) {
		this.parent = parent;
	}
	
	protected SerializationContext parent;
	
	public SerializationContext getParent() {
		return parent;
	}
	
	/** Return the first ObjectContext in the ancestors, or null. */
	public abstract ObjectContext getContainerObjectContext();
	
	/** The context is on an object. */
	public static class ObjectContext extends SerializationContext {
		
		/** Constructor. */
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
	
	/** The context is on an attribute of an object. */
	public static class AttributeContext extends SerializationContext {
		
		/** Constructor. */
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
	
	/** The context is on a collection. */
	public static class CollectionContext extends SerializationContext {
		
		/** Constructor. */
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
		
		/** Get an iterator on the collection. */
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
