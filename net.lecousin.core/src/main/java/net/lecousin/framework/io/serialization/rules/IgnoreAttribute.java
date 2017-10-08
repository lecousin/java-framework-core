package net.lecousin.framework.io.serialization.rules;

import java.util.ArrayList;
import java.util.Iterator;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;

/**
 * This rule ignore a specific attribute in a class, or all attributes if name is null.
 */
public class IgnoreAttribute implements SerializationRule {
	
	/** Constructor. */
	public IgnoreAttribute(Class<?> clazz, String name) {
		this.clazz = clazz;
		this.name = name;
	}
	
	private Class<?> clazz;
	private String name;
	
	@Override
	public void apply(ArrayList<Attribute> attributes) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!clazz.equals(a.getDeclaringClass())) continue;
			if (name != null && !name.equals(a.getOriginalName())) continue;
			a.ignoreSerialization();
		}
	}

}
