package net.lecousin.framework.io.serialization.rules;

import java.util.ArrayList;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationUtil;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
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
	public void apply(ArrayList<Attribute> attributes) {
		for (ListIterator<Attribute> it = attributes.listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!clazz.equals(a.getDeclaringClass())) continue;
			if (!name.equals(a.getOriginalName())) continue;
			Attribute discr = SerializationUtil.getAttributeByOriginalName(attributes, discriminator);
			if (discr == null || !discr.canGet()) {
				// TODO log ?
				continue;
			}
			try {
				it.set(new InstantiationAttribute(a, discr, factory.newInstance()));
			} catch (Throwable t) {
				// TODO log ?
			}
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
