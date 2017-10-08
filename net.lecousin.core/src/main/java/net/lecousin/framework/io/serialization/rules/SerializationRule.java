package net.lecousin.framework.io.serialization.rules;

import java.util.ArrayList;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;

/** Interface for a serialization rule. */
public interface SerializationRule {

	/** Apply the rule to the given attributes. */
	public void apply(ArrayList<Attribute> attributes);
	
}
