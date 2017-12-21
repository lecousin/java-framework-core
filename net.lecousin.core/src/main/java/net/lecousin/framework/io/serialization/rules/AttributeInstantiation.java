package net.lecousin.framework.io.serialization.rules;

import java.util.List;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.util.Factory;

/** Base class specifying a rule on how to instantiate an attribute by providing a factory which
 * will be given the context as parameter to instantiate the attribute. */
@SuppressWarnings("rawtypes")
public class AttributeInstantiation implements SerializationRule {

	/** Constructor. */
	public AttributeInstantiation(OnClassAttribute pattern, Class<? extends Factory> factory) {
		this.pattern = pattern;
		this.factory = factory;
	}

	/** Constructor. */
	public AttributeInstantiation(Class<?> clazz, String name, Class<? extends Factory> factory) {
		this(new OnClassAttribute(clazz, name), factory);
	}
	
	private OnClassAttribute pattern;
	private Class<? extends Factory> factory;
	
	@Override
	public boolean apply(
		SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing
	) throws Exception {
		Attribute a = pattern.getAttribute(type, context);
		if (a == null)
			return false;
		try {
			type.replaceAttribute(a, new InstantiationAttribute(a, factory.newInstance()));
			return false;
		} catch (Throwable t) {
			throw new Exception("Unable to replace attribute by an InstantiationAttribute", t);
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof AttributeInstantiation)) return false;
		AttributeInstantiation r = (AttributeInstantiation)rule;
		return r.pattern.isEquivalent(pattern);
	}
	
	private static class InstantiationAttribute extends Attribute {
		public InstantiationAttribute(Attribute a, Factory factory) {
			super(a);
			this.factory = factory;
		}
		
		private Factory factory;
		
		@SuppressWarnings("unchecked")
		@Override
		public Object instantiate(AttributeContext context) {
			return factory.create(context);
		}
		
		@Override
		public boolean hasCustomInstantiation() {
			return true;
		}
	}
	
}
