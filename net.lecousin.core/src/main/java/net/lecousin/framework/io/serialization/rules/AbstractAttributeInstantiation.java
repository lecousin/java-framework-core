package net.lecousin.framework.io.serialization.rules;

import java.util.List;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.io.serialization.SerializationException;
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
	public boolean apply(
		SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing
	) throws SerializationException {
		Attribute a = pattern.getAttribute(type, context);
		if (a == null)
			return false;
		Attribute discr = type.getAttributeByOriginalName(discriminator);
		if (discr == null || !discr.canGet())
			throw new SerializationException("Unable to get discriminator attribute " + discriminator);
		try {
			type.replaceAttribute(a, new InstantiationAttribute(a, discr, factory.newInstance()));
		} catch (Exception t) {
			throw new SerializationException("Unable to replace attribute by an InstantiationAttribute", t);
		}
		return false;
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
		public Object instantiate(AttributeContext context) throws SerializationException {
			return factory.create(discriminator.getValue(context.getParent().getInstance()));
		}
		
		@Override
		public boolean hasCustomInstantiation() {
			return true;
		}
	}
	
}
