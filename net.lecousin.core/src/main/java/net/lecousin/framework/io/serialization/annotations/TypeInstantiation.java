package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.serialization.rules.TypeFactory;

/** Specify a factory to instantiate the type. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TypeInstantiation {

	/** Factory to create an instance based on the container instance. */
	@SuppressWarnings("rawtypes")
	Class<? extends Supplier> factory();
	
	/** Convert an annotation into a rule. */
	public static class ToRule implements TypeAnnotationToRule<TypeInstantiation> {
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public SerializationRule createRule(TypeInstantiation annotation, Class<?> type) {
			try {
				return new TypeFactory(type, annotation.factory().newInstance());
			} catch (Exception t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Unable to instantiate factory " + annotation.factory().getName(), t
				);
				return null;
			}
		}
		
	}

	
}
