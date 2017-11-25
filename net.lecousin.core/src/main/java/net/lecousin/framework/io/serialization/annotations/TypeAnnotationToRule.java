package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;

import net.lecousin.framework.io.serialization.rules.SerializationRule;

public interface TypeAnnotationToRule<TAnnotation extends Annotation> {

	SerializationRule createRule(TAnnotation annotation, Class<?> type);
	
}
