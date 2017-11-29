package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;

/** Interface for a serialization rule. */
public interface SerializationRule {

	/** Apply the rule to the given type. */
	void apply(SerializationClass type, Object containerInstance);
	
	/** Check if this rule is equivalent to the given rule. */
	boolean isEquivalent(SerializationRule rule);
	
	/** If a SerializationRule implements this interface, it will be called each time a new instance is created during deserialization. */
	interface DeserializationInstanceListener {
		
		void onInstantiation(Object instance);
		
	}
	
}
