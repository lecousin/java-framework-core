package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.plugins.ExtensionPoint;

/**
 * Extension point to add annotations that can be converted into serialization rules.
 */
@SuppressWarnings("rawtypes")
public class AnnotationToRule implements ExtensionPoint<AnnotationPlugin> {

	/** Create the rule associated with the given annotation. */
	@SuppressWarnings("unchecked")
	public static SerializationRule getRule(Attribute attribute, Annotation annotation) {
		for (AnnotationPlugin pi : instance.plugins) {
			if (annotation.annotationType().equals(pi.getAnnotationType()))
				return pi.getRule(attribute, annotation);
		}
		return null;
	}
	
	private static AnnotationToRule instance;
	
	/** Constructor called at application initialization time. */
	public AnnotationToRule() {
		if (instance != null) throw new RuntimeException("Instantiation is forbidden");
		instance = this;
		addPlugin(new Transient.ToRule());
		addPlugin(new CustomAttributeSerialization.ToRule());
		addPlugin(new Instantiation.ToRule());
		addPlugin(new SerializationMethod.ToRule());
	}
	
	private ArrayList<AnnotationPlugin> plugins = new ArrayList<>();
	
	@Override
	public Class<AnnotationPlugin> getPluginClass() { return AnnotationPlugin.class; }

	@Override
	public void addPlugin(AnnotationPlugin plugin) {
		plugins.add(plugin);
	}

	@Override
	public void allPluginsLoaded() {
	}
	
}
