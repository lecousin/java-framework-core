package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.util.Factory;

/** Base class specifying a rule on how to instantiate an attribute. */
@SuppressWarnings("rawtypes")
public class AbstractAttributeInstantiation implements SerializationRule {

	/** Constructor. */
	public AbstractAttributeInstantiation(OnClassAttribute pattern, String discriminator, Class<? extends Factory> factory) {
		this.pattern = pattern;
		this.discriminator = discriminator;
		this.factory = factory;
	}

	/** Constructor. */
	public AbstractAttributeInstantiation(Class<?> clazz, String name, String discriminator, Class<? extends Factory> factory) {
		this(new OnClassAttribute(clazz, name), discriminator, factory);
	}
	
	private OnClassAttribute pattern;
	private String discriminator;
	private Class<? extends Factory> factory;
	
	@Override
	public void apply(SerializationClass type, SerializationContext context) {
		Attribute a = pattern.getAttribute(type, context);
		if (a == null)
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
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof AbstractAttributeInstantiation)) return false;
		AbstractAttributeInstantiation r = (AbstractAttributeInstantiation)rule;
		return r.pattern.isEquivalent(pattern) && r.discriminator.equals(discriminator);
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
		public Object instantiate(AttributeContext context) throws Exception {
			return factory.create(discriminator.getValue(context.getParent().getInstance()));
		}
	}
	
}
