package net.lecousin.framework.io.serialization.rules;

import java.util.List;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.TypeDefinition;

/** Interface for a serialization rule. */
@SuppressWarnings("unused")
public interface SerializationRule {

	/** Apply the rule to the given type, knowing the given context.
	 * If true is returned no other rule will be applied on the type.
	 */
	boolean apply(SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing)
	throws SerializationException;
	
	/** Check if this rule is equivalent to the given rule. */
	boolean isEquivalent(SerializationRule rule);
	
	/** Before to serialize a value, this method is called in order to give the opportunity to convert to another value. */
	default Object convertSerializationValue(Object value, TypeDefinition type, SerializationContext context) throws SerializationException {
		return value;
	}
	
	/** Can be used to change a type during deserialization, in a similar way as the method
	 * convertSerializationValue during serialization. This is also used for writing
	 * serialization specification.
	 */
	default TypeDefinition getDeserializationType(TypeDefinition type, SerializationContext context) {
		return type;
	}
	
	/** Can be used to change the deserialization, in a similar way as the method
	 * convertSerializationValue during serialization.
	 */
	default Object getDeserializationValue(Object value, TypeDefinition type, SerializationContext context) throws SerializationException {
		return value;
	}
	
	/** During deserialization, when a type needs to be instantiated, this method is called to know
	 * if this rule is providing a custom way to instantiate the given type.
	 */
	default boolean canInstantiate(TypeDefinition type, SerializationContext context) {
		return false;
	}

	/** Called if the method canInstantiate previously returned true during deserialization. */
	default Object instantiate(TypeDefinition type, SerializationContext context) throws SerializationException {
		return null;
	}
	
	/** Called each time a type is instantiated during deserialization. */
	default void onInstantiation(TypeDefinition type, Object instance, SerializationContext context) throws SerializationException {
	}
	
}
