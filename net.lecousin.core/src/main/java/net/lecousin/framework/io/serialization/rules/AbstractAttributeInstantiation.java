package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.util.Factory;

/** Base class specifying a rule on how to instantiate an attribute. */
@SuppressWarnings("rawtypes")
public class AbstractAttributeInstantiation implements SerializationRule {

	/** Constructor. */
	public AbstractAttributeInstantiation(Class<?> clazz, String name, String discriminator, Class<? extends Factory> factory) {
		this.clazz = clazz;
		this.name = name;
		this.discriminator = discriminator;
		this.factory = factory;
	}
	
	private Class<?> clazz;
	private String name;
	private String discriminator;
	private Class<? extends Factory> factory;
	
	@Override
	public void apply(SerializationClass type) {
		if (!clazz.isAssignableFrom(type.getType().getBase()))
			return;
		Attribute a = type.getAttributeByOriginalName(name);
		if (a == null || !clazz.equals(a.getDeclaringClass()))
			return;
		Attribute discr = type.getAttributeByOriginalName(discriminator);
		if (discr == null || !discr.canGet()) {
			LCCore.getApplication().getDefaultLogger().warn("Unable to get discriminator attribute " + discriminator);
			return;
		}
		try {
			type.replaceAttribute(a, new InstantiationAttribute(a, discr, factory.newInstance()));
		} catch (Throwable t) {
			LCCore.getApplication().getDefaultLogger().error("Unable to replace attribute by an InstantiationAttribute", t);
		}
	}
	
	private static class InstantiationAttribute extends Attribute {
		public InstantiationAttribute(Attribute a, Attribute discriminator, Factory factory) {
			super(a);
			this.discriminator = discriminator;
			this.factory = factory;
		}
		
		private Attribute discriminator;
		private Factory factory;
		
		@SuppressWarnings("unchecked")
		@Override
		public Object instantiate(Object containerInstance) throws Exception {
			return factory.create(discriminator.getValue(containerInstance));
		}
	}
	
}
