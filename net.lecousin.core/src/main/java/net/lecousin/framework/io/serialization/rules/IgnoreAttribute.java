package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

/**
 * This rule ignore a specific attribute in a class, or all attributes if name is null.
 */
public class IgnoreAttribute implements SerializationRule {

	/** Constructor. */
	public IgnoreAttribute(Class<?> type, String name) {
		this.type = type;
		this.name = name;
	}
	
	private Class<?> type;
	private String name;
	
	@Override
	public void apply(SerializationClass type, Object containerInstance) {
		if (!this.type.isAssignableFrom(type.getType().getBase()))
			return;
		if (name != null) {
			Attribute a = type.getAttributeByOriginalName(name);
			if (a == null || !this.type.equals(a.getDeclaringClass()))
				return;
			a.ignore(true);
		} else
			for (Attribute a : type.getAttributes())
				if (this.type.equals(a.getDeclaringClass()))
					a.ignore(true);
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof IgnoreAttribute)) return false;
		IgnoreAttribute r = (IgnoreAttribute)rule;
		return r.type.equals(type) && r.name.equals(name);
	}

}
