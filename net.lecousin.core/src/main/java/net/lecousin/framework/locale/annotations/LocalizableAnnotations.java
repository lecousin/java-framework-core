package net.lecousin.framework.locale.annotations;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import net.lecousin.framework.locale.FixedLocalizedString;
import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.properties.Property;

/** Utility methods for localization annotations. */
public final class LocalizableAnnotations {

	private LocalizableAnnotations() {}
	
	/** Get a localizable string from the given element. */
	public static ILocalizableString get(AnnotatedElement element, String name) {
		for (LocalizableProperty p : element.getAnnotationsByType(LocalizableProperty.class)) {
			if (!p.name().equals(name)) continue;
			String ns = p.namespace();
			if (ns.length() == 0) {
				LocalizableNamespace lns = element.getAnnotation(LocalizableNamespace.class);
				if (lns == null && element instanceof Member)
					lns = ((Member)element).getDeclaringClass().getAnnotation(LocalizableNamespace.class);
				if (lns != null)
					ns = lns.value();
			}
			String[] values = p.values();
			Serializable[] v = new Serializable[values.length];
			System.arraycopy(values, 0, v, 0, values.length);
			return new LocalizableString(ns, p.key(), v);
		}
		for (Property p : element.getAnnotationsByType(Property.class)) {
			if (!p.name().equals(name)) continue;
			return new FixedLocalizedString(p.value());
		}
		return null;
	}
	
}
