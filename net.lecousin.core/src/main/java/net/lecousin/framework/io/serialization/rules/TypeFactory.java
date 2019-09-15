package net.lecousin.framework.io.serialization.rules;

import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.TypeDefinition;

/** A factory to instantiate a specific type during deserialization.
 * @param <T> type
 */
public class TypeFactory<T> implements SerializationRule {

	/** Constructor. */
	public TypeFactory(Class<T> type, Supplier<T> factory) {
		this.type = type;
		this.factory = factory;
	}
	
	private Class<T> type;
	private Supplier<T> factory;
	
	public Class<T> getType() {
		return type;
	}
	
	public Supplier<T> getFactory() {
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
					return factory.get();
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
		return factory.get();
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof TypeFactory)) return false;
		TypeFactory<?> r = (TypeFactory<?>)rule;
		return r.type.equals(type);
	}
	
}
