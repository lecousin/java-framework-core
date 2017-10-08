package net.lecousin.framework.io.serialization.rules;

import java.util.ArrayList;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;

/**
 * This rule change the name of a specific attribute in a class.
 */
public class RenameAttribute implements SerializationRule {

	/** Constructor. */
	public RenameAttribute(Class<?> clazz, String name, String newName) {
		this.clazz = clazz;
		this.name = name;
		this.newName = newName;
	}
	
	private Class<?> clazz;
	private String name;
	private String newName;
	
	@Override
	public void apply(ArrayList<Attribute> attributes) {
		for (Attribute a : attributes) {
			if (!clazz.equals(a.getDeclaringClass())) continue;
			if (!name.equals(a.getName())) continue;
			a.rename(newName);
		}
	}
	
}
