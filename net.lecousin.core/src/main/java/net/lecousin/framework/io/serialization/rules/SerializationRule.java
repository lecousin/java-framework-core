package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.TypeDefinition;

/** Interface for a serialization rule. */
@SuppressWarnings("unused")
public interface SerializationRule {

	/** Apply the rule to the given type, knowing the given context. */
	void apply(SerializationClass type, SerializationContext context);
	
	/** Check if this rule is equivalent to the given rule. */
	boolean isEquivalent(SerializationRule rule);
	
	/** During deserialization, when a type needs to be instantiated, this method is called to know
	 * if this rule is providing a custom way to instantiate the given type.
	 */
	default boolean canInstantiate(TypeDefinition type, SerializationContext context) {
		return false;
	}

	/** Called if the method canInstantiate previously returned true during deserialization. */
	default Object instantiate(TypeDefinition type, SerializationContext context) throws Exception {
		return null;
	}
	
	/** Called each time a type is instantiated during deserialization. */
	default void onInstantiation(TypeDefinition type, Object instance, SerializationContext context) throws Exception {
	}
	
}
