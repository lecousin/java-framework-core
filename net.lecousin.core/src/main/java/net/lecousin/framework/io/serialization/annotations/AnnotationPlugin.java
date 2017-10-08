package net.lecousin.framework.io.serialization.annotations;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.plugins.Plugin;

/** Interface for plug-ins adding a new serialization annotation.
 * @param <T> type of the annotation
 */
public interface AnnotationPlugin<T> extends Plugin {

	/** Type of annotation. */
	public Class<T> getAnnotationType();
	
	/** Create corresponding rule. */
	public SerializationRule getRule(Attribute attribute, T annotation);
	
}
