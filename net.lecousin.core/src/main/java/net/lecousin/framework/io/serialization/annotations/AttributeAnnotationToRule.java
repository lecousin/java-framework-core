package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public interface AttributeAnnotationToRule<TAnnotation extends Annotation> {

	SerializationRule createRule(TAnnotation annotation, Attribute attribute);
	
}
