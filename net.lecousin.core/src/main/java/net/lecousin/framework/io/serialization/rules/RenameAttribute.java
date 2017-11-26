package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

/**
 * This rule change the name of a specific attribute in a class.
 */
public class RenameAttribute implements SerializationRule {

	/** Constructor. */
	public RenameAttribute(Class<?> type, String originalName, String newName) {
		this.type = type;
		this.originalName = originalName;
		this.newName = newName;
	}
	
	private Class<?> type;
	private String originalName;
	private String newName;
	
	@Override
	public void apply(SerializationClass type, Object containerInstance) {
		if (!this.type.isAssignableFrom(type.getType().getBase()))
			return;
		Attribute a = type.getAttributeByOriginalName(originalName);
		if (a == null || !this.type.equals(a.getDeclaringClass()))
			return;
		a.renameTo(newName);
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof RenameAttribute)) return false;
		RenameAttribute r = (RenameAttribute)rule;
		return r.type.equals(type) && r.originalName.equals(originalName);
	}
	
}
