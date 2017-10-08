package net.lecousin.framework.locale.annotations;

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
				if (lns == null) {
					if (element instanceof Member)
						lns = ((Member)element).getDeclaringClass().getAnnotation(LocalizableNamespace.class);
				}
				if (lns != null)
					ns = lns.value();
			}
			String[] values = p.values();
			Object[] v = new Object[values.length];
			for (int i = 0; i < values.length; ++i) v[i] = values[i];
			return new LocalizableString(ns, p.key(), v);
		}
		for (Property p : element.getAnnotationsByType(Property.class)) {
			if (!p.name().equals(name)) continue;
			return new FixedLocalizedString(p.value());
		}
		return null;
	}
	
}
