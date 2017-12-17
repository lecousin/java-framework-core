package net.lecousin.framework.io.serialization.rules;

import java.util.List;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern;

/**
 * This rule change the name of a specific attribute in a class.
 */
public class RenameAttribute implements SerializationRule {

	/** Constructor. */
	public RenameAttribute(SerializationContextPattern.OnClassAttribute contextPattern, String newName) {
		this.contextPattern = contextPattern;
		this.newName = newName;
	}

	/** Constructor. */
	public RenameAttribute(Class<?> type, String originalName, String newName) {
		this(new SerializationContextPattern.OnClassAttribute(type, originalName), newName);
	}
	
	private SerializationContextPattern.OnClassAttribute contextPattern;
	private String newName;
	
	@Override
	public boolean apply(SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing) {
		Attribute a = contextPattern.getAttribute(type, context);
		if (a != null)
			a.renameTo(newName);
		return false;
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof RenameAttribute)) return false;
		RenameAttribute r = (RenameAttribute)rule;
		return contextPattern.isEquivalent(r.contextPattern);
	}
	
}
