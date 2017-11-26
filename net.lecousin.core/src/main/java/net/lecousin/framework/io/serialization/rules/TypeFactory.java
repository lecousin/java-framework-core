package net.lecousin.framework.io.serialization.rules;

import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.util.Provider;

public class TypeFactory<T> implements SerializationRule {

	public TypeFactory(Class<T> type, Provider<T> factory) {
		this.type = type;
		this.factory = factory;
	}
	
	private Class<T> type;
	private Provider<T> factory;
	
	public Class<T> getType() {
		return type;
	}
	
	public Provider<T> getFactory() {
		return factory;
	}
	
	@Override
	public void apply(SerializationClass type, Object containerInstance) {
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.getOriginalType().equals(type)) continue;
			it.set(new Attribute(a) {
				@Override
				public Object instantiate(Object containerInstance) {
					return factory.provide();
				}
			});
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof TypeFactory)) return false;
		TypeFactory<?> r = (TypeFactory<?>)rule;
		return r.type.equals(type);
	}
	
}
