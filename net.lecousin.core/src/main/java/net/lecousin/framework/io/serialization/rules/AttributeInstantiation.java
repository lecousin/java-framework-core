package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.util.Factory;

/** Base class specifying a rule on how to instantiate an attribute by providing a factory which
 * will be given the container instance as parameter to instantiate the attribute. */
@SuppressWarnings("rawtypes")
public class AttributeInstantiation implements SerializationRule {

	/** Constructor. */
	public AttributeInstantiation(Class<?> clazz, String name, Class<? extends Factory> factory) {
		this.clazz = clazz;
		this.name = name;
		this.factory = factory;
	}
	
	private Class<?> clazz;
	private String name;
	private Class<? extends Factory> factory;
	
	@Override
	public void apply(SerializationClass type, Object containerInstance) {
		if (!clazz.isAssignableFrom(type.getType().getBase()))
			return;
		Attribute a = type.getAttributeByOriginalName(name);
		if (a == null || !clazz.equals(a.getDeclaringClass()))
			return;
		try {
			type.replaceAttribute(a, new InstantiationAttribute(a, factory.newInstance()));
		} catch (Throwable t) {
			LCCore.getApplication().getDefaultLogger().error("Unable to replace attribute by an InstantiationAttribute", t);
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof AttributeInstantiation)) return false;
		AttributeInstantiation r = (AttributeInstantiation)rule;
		return r.clazz.equals(clazz) && r.name.equals(name);
	}
	
	private static class InstantiationAttribute extends Attribute {
		public InstantiationAttribute(Attribute a, Factory factory) {
			super(a);
			this.factory = factory;
		}
		
		private Factory factory;
		
		@SuppressWarnings("unchecked")
		@Override
		public Object instantiate(Object containerInstance) {
			return factory.create(containerInstance);
		}
	}
	
}
