package net.lecousin.framework.io.serialization;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;

public interface SerializationContextPattern {

	boolean matches(SerializationClass type, SerializationContext context);

	boolean matches(SerializationClass type, SerializationContext context, Attribute attribute);
	
	boolean matches(SerializationContext context);
	
	boolean isEquivalent(SerializationContextPattern p);
	
	public static class OnClass implements SerializationContextPattern {
		
		public OnClass(Class<?> clazz) {
			this.clazz = clazz;
		}
		
		protected Class<?> clazz;
		
		public Class<?> getTargetClass() { return clazz; }
		
		@Override
		public boolean matches(SerializationClass type, SerializationContext context) {
			return clazz.isAssignableFrom(type.getType().getBase());
		}
		
		@Override
		public boolean matches(SerializationClass type, SerializationContext context, Attribute attribute) {
			return clazz.equals(attribute.getDeclaringClass());
		}
		
		@Override
		public boolean matches(SerializationContext context) {
			if (context == null) return false;
			if (context instanceof CollectionContext)
				return matches(((CollectionContext)context).getParent());
			if (context instanceof AttributeContext)
				return ((AttributeContext)context).getAttribute().getDeclaringClass().equals(clazz);
			if (context instanceof ObjectContext)
				return clazz.isAssignableFrom(((ObjectContext)context).getSerializationClass().getType().getBase());
			return false;
		}
		
		@Override
		public boolean isEquivalent(SerializationContextPattern p) {
			if (!(p instanceof OnClass)) return false;
			OnClass o = (OnClass)p;
			return clazz.equals(o.clazz);
		}
	}
	
	public static class OnClassAttribute extends OnClass {
		
		public OnClassAttribute(Class<?> clazz, String attributeName) {
			super(clazz);
			this.attributeName = attributeName;
		}
		
		private String attributeName;
		
		public String getAttributeOriginalName() { return attributeName; }
		
		public Attribute getAttribute(SerializationClass type, SerializationContext context) {
			if (!matches(type, context))
				return null;
			Attribute a = type.getAttributeByOriginalName(attributeName);
			if (a == null)
				return null;
			if (!matches(type, context, a))
				return null;
			return a;
		}
		
		@Override
		public boolean matches(SerializationContext context) {
			if (context == null) return false;
			if (context instanceof CollectionContext)
				return matches(((CollectionContext)context).getParent());
			if (context instanceof AttributeContext) {
				Attribute a = ((AttributeContext)context).getAttribute();
				return a.getDeclaringClass().equals(clazz) && a.getOriginalName().equals(attributeName);
			}
			return false;
		}
		
		@Override
		public boolean isEquivalent(SerializationContextPattern p) {
			if (!(p instanceof OnClassAttribute)) return false;
			OnClassAttribute o = (OnClassAttribute)p;
			return clazz.equals(o.clazz) && attributeName.equals(o.attributeName);
		}
		
	}
	
}
