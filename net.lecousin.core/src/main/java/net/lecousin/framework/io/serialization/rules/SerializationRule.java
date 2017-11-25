package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;

/** Interface for a serialization rule. */
public interface SerializationRule {

	/** Apply the rule to the given type. */
	void apply(SerializationClass type);
	
}
