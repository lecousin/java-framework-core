package net.lecousin.framework.io.serialization.rules;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;

/**
 * This rule ignore a specific attribute or all attributes in a class.
 */
public class IgnoreAttribute implements SerializationRule {

	/** Constructor. */
	public IgnoreAttribute(SerializationContextPattern contextPattern) {
		this.contextPattern = contextPattern;
	}

	/** Constructor to ignore a specific attribute. */
	public IgnoreAttribute(Class<?> type, String name) {
		this(new SerializationContextPattern.OnClassAttribute(type, name));
	}

	/** Constructor to ignore all attributes in a class. */
	public IgnoreAttribute(Class<?> type) {
		this(new SerializationContextPattern.OnClass(type));
	}
	
	private SerializationContextPattern contextPattern;
	
	@Override
	public void apply(SerializationClass type, SerializationContext context) {
		if (!contextPattern.matches(type, context))
			return;
		for (Attribute a : type.getAttributes())
			if (contextPattern.matches(type, context, a))
				a.ignore(true);
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof IgnoreAttribute)) return false;
		IgnoreAttribute r = (IgnoreAttribute)rule;
		return contextPattern.isEquivalent(r.contextPattern);
	}

}
