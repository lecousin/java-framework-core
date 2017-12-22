package net.lecousin.framework.io.serialization.rules;

import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.util.Provider;

/** A factory to instantiate a specific type during deserialization.
 * @param <T> type
 */
public class TypeFactory<T> implements SerializationRule {

	/** Constructor. */
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
	public boolean apply(SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing) {
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.getType().getBase().equals(this.type)) continue;
			it.set(new Attribute(a) {
				@Override
				public Object instantiate(AttributeContext context) {
					return factory.provide();
				}
				
				@Override
				public boolean hasCustomInstantiation() {
					return true;
				}
			});
		}
		return false;
	}
	
	@Override
	public boolean canInstantiate(TypeDefinition type, SerializationContext context) {
		return this.type.equals(type.getBase());
	}
	
	@Override
	public Object instantiate(TypeDefinition type, SerializationContext context) {
		return factory.provide();
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof TypeFactory)) return false;
		TypeFactory<?> r = (TypeFactory<?>)rule;
		return r.type.equals(type);
	}
	
}
